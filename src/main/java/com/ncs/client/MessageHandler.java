package com.ncs.client;

import com.ncs.model.User;
import com.ncs.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Xử lý messages nhận từ server
 * Dispatch events đến UI controllers thích hợp
 */
public class MessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);
    
    private final GameClient gameClient;
    
    // Event listeners
    private LoginEventListener loginEventListener;
    private GameEventListener gameEventListener;
    private UserListEventListener userListEventListener;
    private InvitationEventListener invitationEventListener;

    public MessageHandler(GameClient gameClient) {
        this.gameClient = gameClient;
    }

    /**
     * Xử lý message từ server
     */
    public void handleMessage(Message message) {
        try {
            String action = message.getAction();
            
            switch (action) {
                // Authentication responses
                case Message.Actions.LOGIN:
                    handleLoginResponse(message);
                    break;
                    
                case Message.Actions.REGISTER:
                    handleRegisterResponse(message);
                    break;
                    
                case Message.Actions.LOGOUT:
                    handleLogoutResponse(message);
                    break;
                
                // User list responses
                case Message.Actions.GET_ONLINE_USERS:
                    handleOnlineUsersResponse(message);
                    break;
                    
                case Message.Actions.GET_LEADERBOARD:
                    handleLeaderboardResponse(message);
                    break;
                
                // User status notifications
                case Message.Actions.USER_STATUS_CHANGED:
                    handleUserStatusChanged(message);
                    break;
                
                // Invitation related
                case Message.Actions.INVITATION_RECEIVED:
                    handleInvitationReceived(message);
                    break;
                    
                case Message.Actions.INVITATION_RESPONSE:
                    handleInvitationResponse(message);
                    break;
                    
                case Message.Actions.SEND_INVITATION:
                    handleSendInvitationResponse(message);
                    break;
                    
                case Message.Actions.ACCEPT_INVITATION:
                    handleAcceptInvitationResponse(message);
                    break;
                    
                case Message.Actions.REJECT_INVITATION:
                    handleRejectInvitationResponse(message);
                    break;
                
                // Game related
                case Message.Actions.GAME_STARTED:
                    handleGameStarted(message);
                    break;
                    
                case Message.Actions.GAME_ENDED:
                    handleGameEnded(message);
                    break;
                    
                case Message.Actions.SHOW_COLORS:
                    handleShowColors(message);
                    break;
                    
                case Message.Actions.START_GUESSING:
                    handleStartGuessing(message);
                    break;
                    
                case Message.Actions.ROUND_RESULT:
                    handleRoundResult(message);
                    break;
                
                // System messages
                case Message.Actions.ERROR:
                    handleError(message);
                    break;
                    
                case Message.Actions.HEARTBEAT:
                    handleHeartbeat(message);
                    break;
                    
                case "welcome":
                    handleWelcome(message);
                    break;
                    
                default:
                    logger.warn("Unknown message action: {}", action);
            }
            
        } catch (Exception e) {
            logger.error("Lỗi xử lý message {}: {}", message.getAction(), e.getMessage(), e);
        }
    }

    // Authentication handlers
    
    private void handleLoginResponse(Message message) {
        if (loginEventListener != null) {
            if (message.isSuccess()) {
                User user = extractUser(message.getData());
                if (user != null) {
                    gameClient.setCurrentUser(user);
                    loginEventListener.onLoginSuccess(user);
                } else {
                    loginEventListener.onLoginFailure("Dữ liệu user không hợp lệ");
                }
            } else {
                loginEventListener.onLoginFailure(message.getMessage());
            }
        }
    }

    private void handleRegisterResponse(Message message) {
        if (loginEventListener != null) {
            if (message.isSuccess()) {
                User user = extractUser(message.getData());
                loginEventListener.onRegisterSuccess(user);
            } else {
                loginEventListener.onRegisterFailure(message.getMessage());
            }
        }
    }

    private void handleLogoutResponse(Message message) {
        gameClient.setCurrentUser(null);
        if (loginEventListener != null) {
            loginEventListener.onLogout();
        }
    }

    // User list handlers
    
    private void handleOnlineUsersResponse(Message message) {
        if (userListEventListener != null && message.isSuccess()) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> usersData = 
                (java.util.List<Map<String, Object>>) message.getData().get("users");
            
            if (usersData != null) {
                java.util.List<User> users = new java.util.ArrayList<>();
                for (Map<String, Object> userData : usersData) {
                    User user = mapToUser(userData);
                    if (user != null) {
                        users.add(user);
                    }
                }
                userListEventListener.onOnlineUsersReceived(users);
            }
        }
    }

    private void handleLeaderboardResponse(Message message) {
        if (userListEventListener != null && message.isSuccess()) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> leaderboardData = 
                (java.util.List<Map<String, Object>>) message.getData().get("leaderboard");
            
            if (leaderboardData != null) {
                java.util.List<User> leaderboard = new java.util.ArrayList<>();
                for (Map<String, Object> userData : leaderboardData) {
                    User user = mapToUser(userData);
                    if (user != null) {
                        leaderboard.add(user);
                    }
                }
                userListEventListener.onLeaderboardReceived(leaderboard);
            }
        }
    }

    private void handleUserStatusChanged(Message message) {
        if (userListEventListener != null) {
            User user = extractUser(message.getData(), "user");
            String action = (String) message.getData().get("action");
            
            if (user != null && action != null) {
                userListEventListener.onUserStatusChanged(user, action);
            }
        }
    }

    // Invitation handlers
    
    private void handleInvitationReceived(Message message) {
        if (invitationEventListener != null) {
            // Extract invitation data
            @SuppressWarnings("unchecked")
            Map<String, Object> invitationData = (Map<String, Object>) message.getData().get("invitation");
            User inviter = extractUser(message.getData(), "inviter");
            
            if (invitationData != null && inviter != null) {
                invitationEventListener.onInvitationReceived(invitationData, inviter);
            }
        }
    }

    private void handleInvitationResponse(Message message) {
        if (invitationEventListener != null) {
            String response = (String) message.getData().get("response");
            User responder = extractUser(message.getData(), "accepter");
            if (responder == null) {
                responder = extractUser(message.getData(), "rejecter");
            }
            
            if (response != null) {
                invitationEventListener.onInvitationResponse(response, responder);
            }
        }
    }

    private void handleSendInvitationResponse(Message message) {
        if (invitationEventListener != null) {
            if (message.isSuccess()) {
                invitationEventListener.onInvitationSent(message.getMessage());
            } else {
                invitationEventListener.onInvitationError(message.getMessage());
            }
        }
    }

    private void handleAcceptInvitationResponse(Message message) {
        if (invitationEventListener != null) {
            if (message.isSuccess()) {
                invitationEventListener.onInvitationAccepted();
            } else {
                invitationEventListener.onInvitationError(message.getMessage());
            }
        }
    }

    private void handleRejectInvitationResponse(Message message) {
        if (invitationEventListener != null) {
            if (message.isSuccess()) {
                invitationEventListener.onInvitationRejected(message.getMessage());
            } else {
                invitationEventListener.onInvitationError(message.getMessage());
            }
        }
    }

    // Game handlers
    
    private void handleGameStarted(Message message) {
        if (gameEventListener != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> gameData = (Map<String, Object>) message.getData().get("game");
            gameEventListener.onGameStarted(gameData);
        }
    }

    private void handleGameEnded(Message message) {
        if (gameEventListener != null) {
            String reason = (String) message.getData().get("reason");
            Integer winnerId = (Integer) message.getData().get("winnerId");
            gameEventListener.onGameEnded(reason, winnerId);
        }
    }

    private void handleShowColors(Message message) {
        if (gameEventListener != null) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> colorsData = 
                (java.util.List<Map<String, Object>>) message.getData().get("colors");
            Integer duration = (Integer) message.getData().get("duration");
            Integer roundNumber = (Integer) message.getData().get("roundNumber");
            
            if (colorsData != null) {
                java.util.List<com.ncs.model.GameColor> colors = new java.util.ArrayList<>();
                for (Map<String, Object> colorData : colorsData) {
                    Integer r = (Integer) colorData.get("r");
                    Integer g = (Integer) colorData.get("g");
                    Integer b = (Integer) colorData.get("b");
                    if (r != null && g != null && b != null) {
                        colors.add(new com.ncs.model.GameColor(r, g, b));
                    }
                }
                gameEventListener.onShowColors(colors, duration != null ? duration : 5000, 
                                              roundNumber != null ? roundNumber : 1);
            }
        }
    }

    private void handleStartGuessing(Message message) {
        if (gameEventListener != null) {
            @SuppressWarnings("unchecked")
            java.util.List<Map<String, Object>> paletteData = 
                (java.util.List<Map<String, Object>>) message.getData().get("colorPalette");
            Integer duration = (Integer) message.getData().get("duration");
            Integer roundNumber = (Integer) message.getData().get("roundNumber");
            
            if (paletteData != null) {
                java.util.List<com.ncs.model.GameColor> palette = new java.util.ArrayList<>();
                for (Map<String, Object> colorData : paletteData) {
                    Integer r = (Integer) colorData.get("r");
                    Integer g = (Integer) colorData.get("g");
                    Integer b = (Integer) colorData.get("b");
                    if (r != null && g != null && b != null) {
                        palette.add(new com.ncs.model.GameColor(r, g, b));
                    }
                }
                gameEventListener.onStartGuessing(palette, duration != null ? duration : 10000,
                                                 roundNumber != null ? roundNumber : 1);
            }
        }
    }

    private void handleRoundResult(Message message) {
        if (gameEventListener != null) {
            Integer roundNumber = (Integer) message.getData().get("roundNumber");
            Boolean player1Correct = (Boolean) message.getData().get("player1Correct");
            Boolean player2Correct = (Boolean) message.getData().get("player2Correct");
            Integer winnerId = (Integer) message.getData().get("winnerId");
            Double totalScore1 = (Double) message.getData().get("totalScore1");
            Double totalScore2 = (Double) message.getData().get("totalScore2");
            
            gameEventListener.onRoundResult(roundNumber, player1Correct, player2Correct, 
                                          winnerId, totalScore1, totalScore2);
        }
    }

    // System handlers
    
    private void handleError(Message message) {
        logger.error("Server error: {}", message.getMessage());
        // Could notify UI about error
    }

    private void handleHeartbeat(Message message) {
        // Heartbeat response - connection is alive
        logger.debug("Heartbeat response received");
    }

    private void handleWelcome(Message message) {
        String welcomeMessage = (String) message.getData().get("message");
        logger.info("Welcome message: {}", welcomeMessage);
    }

    // Helper methods
    
    private User extractUser(Map<String, Object> data) {
        return extractUser(data, "user");
    }

    private User extractUser(Map<String, Object> data, String key) {
        if (data == null) return null;
        
        @SuppressWarnings("unchecked")
        Map<String, Object> userData = (Map<String, Object>) data.get(key);
        return mapToUser(userData);
    }

    private User mapToUser(Map<String, Object> userData) {
        if (userData == null) return null;
        
        try {
            User user = new User();
            
            if (userData.get("id") instanceof Number) {
                user.setId(((Number) userData.get("id")).intValue());
            }
            
            user.setUsername((String) userData.get("username"));
            user.setEmail((String) userData.get("email"));
            user.setFullName((String) userData.get("fullName"));
            
            if (userData.get("rankingPoints") instanceof Number) {
                user.setRankingPoints(((Number) userData.get("rankingPoints")).intValue());
            }
            
            if (userData.get("totalGames") instanceof Number) {
                user.setTotalGames(((Number) userData.get("totalGames")).intValue());
            }
            
            if (userData.get("wins") instanceof Number) {
                user.setWins(((Number) userData.get("wins")).intValue());
            }
            
            if (userData.get("losses") instanceof Number) {
                user.setLosses(((Number) userData.get("losses")).intValue());
            }
            
            if (userData.get("draws") instanceof Number) {
                user.setDraws(((Number) userData.get("draws")).intValue());
            }
            
            if (userData.get("online") instanceof Boolean) {
                user.setOnline((Boolean) userData.get("online"));
            }
            
            String status = (String) userData.get("status");
            if (status != null) {
                try {
                    user.setStatus(User.UserStatus.valueOf(status.toUpperCase()));
                } catch (IllegalArgumentException e) {
                    user.setStatus(User.UserStatus.OFFLINE);
                }
            }
            
            return user;
            
        } catch (Exception e) {
            logger.error("Lỗi map userData to User: {}", e.getMessage());
            return null;
        }
    }

    // Event listener setters
    
    public void setLoginEventListener(LoginEventListener loginEventListener) {
        this.loginEventListener = loginEventListener;
    }

    public void setGameEventListener(GameEventListener gameEventListener) {
        this.gameEventListener = gameEventListener;
    }

    public void setUserListEventListener(UserListEventListener userListEventListener) {
        this.userListEventListener = userListEventListener;
    }

    public void setInvitationEventListener(InvitationEventListener invitationEventListener) {
        this.invitationEventListener = invitationEventListener;
    }

    // Event listener interfaces
    
    public interface LoginEventListener {
        void onLoginSuccess(User user);
        void onLoginFailure(String message);
        void onRegisterSuccess(User user);
        void onRegisterFailure(String message);
        void onLogout();
    }

    public interface GameEventListener {
        void onGameStarted(Map<String, Object> gameData);
        void onGameEnded(String reason, Integer winnerId);
        void onShowColors(java.util.List<com.ncs.model.GameColor> colors, int duration, int roundNumber);
        void onStartGuessing(java.util.List<com.ncs.model.GameColor> colorPalette, int duration, int roundNumber);
        void onRoundResult(Integer roundNumber, Boolean player1Correct, Boolean player2Correct, 
                          Integer winnerId, Double totalScore1, Double totalScore2);
    }

    public interface UserListEventListener {
        void onOnlineUsersReceived(java.util.List<User> users);
        void onLeaderboardReceived(java.util.List<User> leaderboard);
        void onUserStatusChanged(User user, String action);
    }

    public interface InvitationEventListener {
        void onInvitationReceived(Map<String, Object> invitationData, User inviter);
        void onInvitationResponse(String response, User responder);
        void onInvitationSent(String message);
        void onInvitationAccepted();
        void onInvitationRejected(String message);
        void onInvitationError(String message);
    }
}

