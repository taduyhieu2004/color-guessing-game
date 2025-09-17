package com.ncs.server;

import com.ncs.dao.GameDAO;
import com.ncs.dao.UserDAO;
import com.ncs.model.Game;
import com.ncs.model.GameInvitation;
import com.ncs.model.User;
import com.ncs.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Quản lý tất cả các game session và game logic
 * Xử lý invite, matchmaking, game rounds, scoring
 */
public class GameManager {
  private static final Logger logger = LoggerFactory.getLogger(GameManager.class);

  public final GameServer server;
  public final UserDAO userDAO;
  public final GameDAO gameDAO;
//
//  public UserDAO getUserDAO() {
//    return userDAO;
//  }
//
//  public GameDAO getGameDAO() {
//    return gameDAO;
//  }
//
//  public GameServer getServer() {
//    return server;
//  }

  // Game management
  private final ConcurrentHashMap<Integer, GameSession> activeGames = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Integer, GameInvitation> pendingInvitations = new ConcurrentHashMap<>();

  // Rematch management
  private final ConcurrentHashMap<String, RematchRequest> rematchRequests = new ConcurrentHashMap<>();

  // Scheduled tasks
  private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(5);
  private final ExecutorService gameThreadPool = Executors.newCachedThreadPool();

  // Game configuration
  private static final int SHOW_COLORS_DURATION = 5000; // 5 giây hiển thị màu
  private static final int GUESSING_DURATION = 10000;   // 10 giây đoán màu
  private static final int INVITATION_TIMEOUT = 120000; // 2 phút timeout cho invitation
  private static final int REMATCH_TIMEOUT = 10000;     // 10 giây timeout cho rematch

  public GameManager(GameServer server, UserDAO userDAO, GameDAO gameDAO) {
    this.server = server;
    this.userDAO = userDAO;
    this.gameDAO = gameDAO;
  }

  /**
   * Khởi động GameManager
   */
  public void start() {
    logger.info("GameManager started");

    // Schedule cleanup tasks
    scheduler.scheduleAtFixedRate(this::cleanupExpiredInvitations, 30, 30, TimeUnit.SECONDS);
    scheduler.scheduleAtFixedRate(this::cleanupExpiredRematchRequests, 15, 15, TimeUnit.SECONDS);
  }

  /**
   * Dừng GameManager
   */
  public void stop() {
    logger.info("Stopping GameManager...");

    // Dừng tất cả games
    for (GameSession session : activeGames.values()) {
      session.forceEnd("Server shutdown");
    }
    activeGames.clear();

    // Clear pending invitations
    pendingInvitations.clear();
    rematchRequests.clear();

    // Shutdown schedulers
    scheduler.shutdown();
    gameThreadPool.shutdown();

    try {
      if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
        scheduler.shutdownNow();
      }
      if (!gameThreadPool.awaitTermination(5, TimeUnit.SECONDS)) {
        gameThreadPool.shutdownNow();
      }
    } catch (InterruptedException e) {
      scheduler.shutdownNow();
      gameThreadPool.shutdownNow();
    }

    logger.info("GameManager stopped");
  }

  /**
   * Xử lý gửi lời mời thách đấu
   */
  public void handleSendInvitation(User inviter, String targetUsername, String message) {
    User target = userDAO.findByUsername(targetUsername);
    if (target == null) {
      sendErrorToUser(inviter.getId(), Message.Actions.SEND_INVITATION, "Người dùng không tồn tại");
      return;
    }

    if (!server.isUserOnline(target.getId())) {
      sendErrorToUser(inviter.getId(), Message.Actions.SEND_INVITATION, "Người dùng không online");
      return;
    }

    if (target.getId() == inviter.getId()) {
      sendErrorToUser(inviter.getId(), Message.Actions.SEND_INVITATION, "Không thể mời chính mình");
      return;
    }

    // Kiểm tra user đã trong game chưa
    if (isUserInGame(inviter.getId()) || isUserInGame(target.getId())) {
      sendErrorToUser(inviter.getId(), Message.Actions.SEND_INVITATION, "Người chơi đang trong trận đấu khác");
      return;
    }

    // Tạo invitation
    GameInvitation invitation = new GameInvitation(inviter, target, message);
    pendingInvitations.put(invitation.getId(), invitation);

    // Gửi notification cho target
    Message inviteNotification = Message.notification(Message.Actions.INVITATION_RECEIVED, null);
    inviteNotification.putData("invitation", invitation);
    inviteNotification.putData("inviter", inviter);
    server.sendMessageToUser(target.getId(), inviteNotification);

    // Confirm cho inviter
    Message response = Message.response(Message.Actions.SEND_INVITATION, null, true);
    response.putData("message", "Đã gửi lời mời thành công");
    server.sendMessageToUser(inviter.getId(), response);

    // Schedule timeout
    scheduler.schedule(() -> {
      GameInvitation inv = pendingInvitations.get(invitation.getId());
      if (inv != null && inv.isPending()) {
        inv.expire();
        pendingInvitations.remove(invitation.getId());

        // Notify both users
        Message timeoutMsg = Message.notification("invitation_expired", null);
        timeoutMsg.putData("invitation", inv);
        server.sendMessageToUser(inviter.getId(), timeoutMsg);
        server.sendMessageToUser(target.getId(), timeoutMsg);
      }
    }, INVITATION_TIMEOUT, TimeUnit.MILLISECONDS);

    logger.info("User {} mời {} thách đấu", inviter.getUsername(), target.getUsername());
  }

  /**
   * Xử lý chấp nhận lời mời
   */
  public void handleAcceptInvitation(User accepter, int invitationId) {
    GameInvitation invitation = pendingInvitations.get(invitationId);
    if (invitation == null || !invitation.isPending()) {
      sendErrorToUser(accepter.getId(), Message.Actions.ACCEPT_INVITATION, "Lời mời không hợp lệ hoặc đã hết hạn");
      return;
    }

    if (invitation.getInviteeId() != accepter.getId()) {
      sendErrorToUser(accepter.getId(), Message.Actions.ACCEPT_INVITATION, "Lời mời không dành cho bạn");
      return;
    }

    // Kiểm tra cả hai user còn online và không trong game khác
    if (!server.isUserOnline(invitation.getInviterId()) || !server.isUserOnline(invitation.getInviteeId())) {
      sendErrorToUser(accepter.getId(), Message.Actions.ACCEPT_INVITATION, "Người chơi đã offline");
      return;
    }

    if (isUserInGame(invitation.getInviterId()) || isUserInGame(invitation.getInviteeId())) {
      sendErrorToUser(accepter.getId(), Message.Actions.ACCEPT_INVITATION, "Người chơi đang trong trận đấu khác");
      return;
    }

    // Accept invitation
    invitation.accept();
    pendingInvitations.remove(invitationId);

    // Tạo game mới
    Game game = gameDAO.createGame(invitation.getInviterId(), invitation.getInviteeId());
    if (game != null) {
      invitation.setGameId(game.getId());

      // Tạo game session
      GameSession session = new GameSession(this, game);
      activeGames.put(game.getId(), session);

      // Notify both users
      Message gameStarted = Message.notification(Message.Actions.GAME_STARTED, null);
      gameStarted.putData("game", game);
      server.sendMessageToUser(invitation.getInviterId(), gameStarted);
      server.sendMessageToUser(invitation.getInviteeId(), gameStarted);

      // Bắt đầu game
      gameThreadPool.submit(() -> session.start());

      logger.info("Game {} bắt đầu: {} vs {}", game.getId(),
            game.getPlayer1().getUsername(), game.getPlayer2().getUsername());
    } else {
      sendErrorToUser(accepter.getId(), Message.Actions.ACCEPT_INVITATION, "Lỗi tạo game");
    }

    // Notify inviter về accept
    Message acceptNotification = Message.notification(Message.Actions.INVITATION_RESPONSE, null);
    acceptNotification.putData("response", "accepted");
    acceptNotification.putData("accepter", accepter);
    server.sendMessageToUser(invitation.getInviterId(), acceptNotification);
  }

  /**
   * Xử lý từ chối lời mời
   */
  public void handleRejectInvitation(User rejecter, int invitationId) {
    GameInvitation invitation = pendingInvitations.get(invitationId);
    if (invitation == null || !invitation.isPending()) {
      sendErrorToUser(rejecter.getId(), Message.Actions.REJECT_INVITATION, "Lời mời không hợp lệ hoặc đã hết hạn");
      return;
    }

    if (invitation.getInviteeId() != rejecter.getId()) {
      sendErrorToUser(rejecter.getId(), Message.Actions.REJECT_INVITATION, "Lời mời không dành cho bạn");
      return;
    }

    // Reject invitation
    invitation.reject();
    pendingInvitations.remove(invitationId);

    // Notify inviter
    Message rejectNotification = Message.notification(Message.Actions.INVITATION_RESPONSE, null);
    rejectNotification.putData("response", "rejected");
    rejectNotification.putData("rejecter", rejecter);
    server.sendMessageToUser(invitation.getInviterId(), rejectNotification);

    // Confirm cho rejecter
    Message response = Message.response(Message.Actions.REJECT_INVITATION, null, true);
    response.putData("message", "Đã từ chối lời mời");
    server.sendMessageToUser(rejecter.getId(), response);

    logger.info("User {} từ chối lời mời từ {}", rejecter.getUsername(), invitation.getInviter().getUsername());
  }

  /**
   * Xử lý submit guess
   */
  public void handleSubmitGuess(User user, Message message) {
    GameSession session = findGameSessionByUser(user.getId());
    if (session == null) {
      sendErrorToUser(user.getId(), Message.Actions.SUBMIT_GUESS, "Bạn không trong game nào");
      return;
    }

    session.handlePlayerGuess(user, message);
  }

  /**
   * Xử lý leave game
   */
  public void handleLeaveGame(User user) {
    GameSession session = findGameSessionByUser(user.getId());
    if (session == null) {
      sendErrorToUser(user.getId(), Message.Actions.LEAVE_GAME, "Bạn không trong game nào");
      return;
    }

    session.handlePlayerLeave(user);
  }

  /**
   * Xử lý user disconnect
   */
  public void handleUserDisconnect(User user) {
    // Remove từ game nếu đang chơi
    GameSession session = findGameSessionByUser(user.getId());
    if (session != null) {
      session.handlePlayerDisconnect(user);
    }

    // Cancel pending invitations
    List<GameInvitation> toCancel = new ArrayList<>();
    for (GameInvitation invitation : pendingInvitations.values()) {
      if (invitation.getInviterId() == user.getId() || invitation.getInviteeId() == user.getId()) {
        toCancel.add(invitation);
      }
    }

    for (GameInvitation invitation : toCancel) {
      invitation.expire();
      pendingInvitations.remove(invitation.getId());
    }
  }

  /**
   * Xử lý request rematch
   */
  public void handleRequestRematch(User user) {
    // Implementation for rematch logic
    logger.info("User {} yêu cầu rematch", user.getUsername());
  }

  /**
   * Xử lý accept rematch
   */
  public void handleAcceptRematch(User user) {
    // Implementation for accept rematch logic
    logger.info("User {} chấp nhận rematch", user.getUsername());
  }

  /**
   * Xử lý reject rematch
   */
  public void handleRejectRematch(User user) {
    // Implementation for reject rematch logic
    logger.info("User {} từ chối rematch", user.getUsername());
  }

  /**
   * Tìm game session theo user
   */
  private GameSession findGameSessionByUser(int userId) {
    for (GameSession session : activeGames.values()) {
      if (session.getGame().isPlayer(userId)) {
        return session;
      }
    }
    return null;
  }

  /**
   * Kiểm tra user có đang trong game không
   */
  private boolean isUserInGame(int userId) {
    return findGameSessionByUser(userId) != null;
  }

  /**
   * Gửi error message đến user
   */
  private void sendErrorToUser(int userId, String action, String errorMessage) {
    server.sendMessageToUser(userId, Message.error(action, errorMessage));
  }

  /**
   * Cleanup expired invitations
   */
  private void cleanupExpiredInvitations() {
    List<Integer> toRemove = new ArrayList<>();
    for (Map.Entry<Integer, GameInvitation> entry : pendingInvitations.entrySet()) {
      if (entry.getValue().isExpired()) {
        toRemove.add(entry.getKey());
      }
    }

    for (Integer id : toRemove) {
      pendingInvitations.remove(id);
    }

    if (!toRemove.isEmpty()) {
      logger.debug("Cleaned up {} expired invitations", toRemove.size());
    }
  }

  /**
   * Cleanup expired rematch requests
   */
  private void cleanupExpiredRematchRequests() {
    // Implementation for cleaning up expired rematch requests
  }

  /**
   * Game kết thúc, remove khỏi active games
   */
  public void onGameFinished(GameSession session) {
    activeGames.remove(session.getGame().getId());
    logger.info("Game {} đã kết thúc", session.getGame().getId());
  }

  /**
   * Lấy số game đang active
   */
  public int getActiveGameCount() {
    return activeGames.size();
  }

  /**
   * Lấy số invitation đang pending
   */
  public int getPendingInvitationCount() {
    return pendingInvitations.size();
  }

  /**
   * Rematch request class
   */
  private static class RematchRequest {
    private final int gameId;
    private final int requesterId;
    private final long timestamp;
    private boolean responded = false;

    public RematchRequest(int gameId, int requesterId) {
      this.gameId = gameId;
      this.requesterId = requesterId;
      this.timestamp = System.currentTimeMillis();
    }

    public boolean isExpired() {
      return System.currentTimeMillis() - timestamp > 10000; // 10 seconds
    }

    // Getters
    public int getGameId() {
      return gameId;
    }

    public int getRequesterId() {
      return requesterId;
    }

    public boolean isResponded() {
      return responded;
    }

    public void setResponded(boolean responded) {
      this.responded = responded;
    }
  }
}
