package com.ncs.server;

import com.ncs.dao.GameDAO;
import com.ncs.dao.UserDAO;
import com.ncs.model.Game;
import com.ncs.model.GameInvitation;
import com.ncs.model.User;
import com.ncs.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handler cho mỗi client connection
 * Xử lý communication và authentication cho từng client
 */
public class ClientHandler {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);
    
    private final GameServer server;
    private final Socket clientSocket;
    private final UserDAO userDAO;
    private final GameDAO gameDAO;
    
    private BufferedReader reader;
    private PrintWriter writer;
    private User user; // User hiện tại đã login
    private final AtomicBoolean connected = new AtomicBoolean(true);
    
    public ClientHandler(GameServer server, Socket clientSocket, UserDAO userDAO, GameDAO gameDAO) {
        this.server = server;
        this.clientSocket = clientSocket;
        this.userDAO = userDAO;
        this.gameDAO = gameDAO;
    }

    /**
     * Bắt đầu xử lý client connection
     */
    public void start() {
        try {
            // Thiết lập I/O streams
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), "UTF-8"), true);
            
            logger.info("ClientHandler started for {}", getClientAddress());
            
            // Gửi welcome message
            Message welcomeMsg = Message.response("welcome", null, true);
            welcomeMsg.putData("message", "Chào mừng đến với Color Guessing Game!");
            sendMessage(welcomeMsg);
            
            // Xử lý messages từ client
            String line;
            while (connected.get() && (line = reader.readLine()) != null) {
                try {
                    Message message = Message.fromJson(line);
                    handleMessage(message);
                } catch (Exception e) {
                    logger.error("Lỗi xử lý message từ client {}: {}", getClientAddress(), e.getMessage());
                    sendErrorMessage("invalid_message", "Format message không hợp lệ");
                }
            }
            
        } catch (SocketException e) {
            logger.info("Client {} đã ngắt kết nối", getClientAddress());
        } catch (IOException e) {
            logger.error("Lỗi I/O với client {}: {}", getClientAddress(), e.getMessage());
        } finally {
            disconnect();
        }
    }

    /**
     * Xử lý message từ client
     */
    private void handleMessage(Message message) {
        String action = message.getAction();
        logger.debug("Received message: {} from {}", action, getClientAddress());
        
        try {
            switch (action) {
                case Message.Actions.LOGIN:
                    handleLogin(message);
                    break;
                    
                case Message.Actions.REGISTER:
                    handleRegister(message);
                    break;
                    
                case Message.Actions.LOGOUT:
                    handleLogout(message);
                    break;
                    
                case Message.Actions.GET_ONLINE_USERS:
                    handleGetOnlineUsers(message);
                    break;
                    
                case Message.Actions.GET_LEADERBOARD:
                    handleGetLeaderboard(message);
                    break;
                    
                case Message.Actions.SEND_INVITATION:
                    handleSendInvitation(message);
                    break;
                    
                case Message.Actions.ACCEPT_INVITATION:
                    handleAcceptInvitation(message);
                    break;
                    
                case Message.Actions.REJECT_INVITATION:
                    handleRejectInvitation(message);
                    break;
                    
                case Message.Actions.SUBMIT_GUESS:
                    handleSubmitGuess(message);
                    break;
                    
                case Message.Actions.LEAVE_GAME:
                    handleLeaveGame(message);
                    break;
                    
                case Message.Actions.REQUEST_REMATCH:
                    handleRequestRematch(message);
                    break;
                    
                case Message.Actions.ACCEPT_REMATCH:
                    handleAcceptRematch(message);
                    break;
                    
                case Message.Actions.REJECT_REMATCH:
                    handleRejectRematch(message);
                    break;
                    
                case Message.Actions.HEARTBEAT:
                    handleHeartbeat(message);
                    break;
                    
                default:
                    sendErrorMessage(action, "Action không được hỗ trợ: " + action);
            }
        } catch (Exception e) {
            logger.error("Lỗi xử lý action {}: {}", action, e.getMessage(), e);
            sendErrorMessage(action, "Lỗi server: " + e.getMessage());
        }
    }

    /**
     * Xử lý login
     */
    private void handleLogin(Message message) {
        if (user != null) {
            sendErrorMessage(Message.Actions.LOGIN, "Bạn đã đăng nhập rồi");
            return;
        }
        
        String username = message.getData("username", String.class);
        String password = message.getData("password", String.class);
        
        if (username == null || password == null) {
            sendErrorMessage(Message.Actions.LOGIN, "Username và password không được để trống");
            return;
        }
        
        // Kiểm tra user đã online chưa
        if (server.isUserOnline(username)) {
            sendErrorMessage(Message.Actions.LOGIN, "User đã đăng nhập từ thiết bị khác");
            return;
        }
        
        User authenticatedUser = userDAO.authenticateUser(username, password);
        if (authenticatedUser != null) {
            this.user = authenticatedUser;
            server.registerClient(this, user);
            
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", user);
            
            sendMessage(Message.response(Message.Actions.LOGIN, responseData, true));
            logger.info("User {} login thành công từ {}", username, getClientAddress());
        } else {
            sendErrorMessage(Message.Actions.LOGIN, "Username hoặc password không đúng");
        }
    }

    /**
     * Xử lý register
     */
    private void handleRegister(Message message) {
        if (user != null) {
            sendErrorMessage(Message.Actions.REGISTER, "Bạn đã đăng nhập rồi");
            return;
        }
        
        String username = message.getData("username", String.class);
        String password = message.getData("password", String.class);
        String email = message.getData("email", String.class);
        String fullName = message.getData("fullName", String.class);
        
        if (username == null || password == null) {
            sendErrorMessage(Message.Actions.REGISTER, "Username và password không được để trống");
            return;
        }
        
        // Kiểm tra username đã tồn tại
        if (userDAO.isUsernameExists(username)) {
            sendErrorMessage(Message.Actions.REGISTER, "Username đã tồn tại");
            return;
        }
        
        // Kiểm tra email đã tồn tại (nếu có)
        if (email != null && !email.trim().isEmpty() && userDAO.isEmailExists(email)) {
            sendErrorMessage(Message.Actions.REGISTER, "Email đã được sử dụng");
            return;
        }
        
        User newUser = new User(username, password);
        newUser.setEmail(email);
        newUser.setFullName(fullName);
        
        if (userDAO.createUser(newUser)) {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("user", newUser);
            
            sendMessage(Message.response(Message.Actions.REGISTER, responseData, true));
            logger.info("User {} đăng ký thành công từ {}", username, getClientAddress());
        } else {
            sendErrorMessage(Message.Actions.REGISTER, "Lỗi tạo tài khoản");
        }
    }

    /**
     * Xử lý logout
     */
    private void handleLogout(Message message) {
        if (user == null) {
            sendErrorMessage(Message.Actions.LOGOUT, "Bạn chưa đăng nhập");
            return;
        }
        
        server.unregisterClient(this);
        this.user = null;
        
        sendMessage(Message.response(Message.Actions.LOGOUT, null, true));
        logger.info("User logout từ {}", getClientAddress());
    }

    /**
     * Xử lý get online users
     */
    private void handleGetOnlineUsers(Message message) {
        if (!isAuthenticated()) return;
        
        List<User> onlineUsers = userDAO.getOnlineUsers();
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("users", onlineUsers);
        
        sendMessage(Message.response(Message.Actions.GET_ONLINE_USERS, responseData, true));
    }

    /**
     * Xử lý get leaderboard
     */
    private void handleGetLeaderboard(Message message) {
        if (!isAuthenticated()) return;
        
        Integer limit = message.getData("limit", Integer.class);
        if (limit == null) limit = 20;
        
        List<User> leaderboard = userDAO.getLeaderboard(limit);
        Map<String, Object> responseData = new HashMap<>();
        responseData.put("leaderboard", leaderboard);
        
        sendMessage(Message.response(Message.Actions.GET_LEADERBOARD, responseData, true));
    }

    /**
     * Xử lý gửi lời mời thách đấu
     */
    private void handleSendInvitation(Message message) {
        if (!isAuthenticated()) return;
        
        String targetUsername = message.getData("targetUsername", String.class);
        if (targetUsername == null) {
            sendErrorMessage(Message.Actions.SEND_INVITATION, "Target username không được để trống");
            return;
        }
        
        // Delegate to GameManager
        server.getGameManager().handleSendInvitation(user, targetUsername, message.getData("message", String.class));
    }

    /**
     * Xử lý chấp nhận lời mời
     */
    private void handleAcceptInvitation(Message message) {
        if (!isAuthenticated()) return;
        
        Integer invitationId = message.getData("invitationId", Integer.class);
        if (invitationId == null) {
            sendErrorMessage(Message.Actions.ACCEPT_INVITATION, "Invitation ID không hợp lệ");
            return;
        }
        
        server.getGameManager().handleAcceptInvitation(user, invitationId);
    }

    /**
     * Xử lý từ chối lời mời
     */
    private void handleRejectInvitation(Message message) {
        if (!isAuthenticated()) return;
        
        Integer invitationId = message.getData("invitationId", Integer.class);
        if (invitationId == null) {
            sendErrorMessage(Message.Actions.REJECT_INVITATION, "Invitation ID không hợp lệ");
            return;
        }
        
        server.getGameManager().handleRejectInvitation(user, invitationId);
    }

    /**
     * Xử lý submit guess
     */
    private void handleSubmitGuess(Message message) {
        if (!isAuthenticated()) return;
        
        server.getGameManager().handleSubmitGuess(user, message);
    }

    /**
     * Xử lý leave game
     */
    private void handleLeaveGame(Message message) {
        if (!isAuthenticated()) return;
        
        server.getGameManager().handleLeaveGame(user);
    }

    /**
     * Xử lý request rematch
     */
    private void handleRequestRematch(Message message) {
        if (!isAuthenticated()) return;
        
        server.getGameManager().handleRequestRematch(user);
    }

    /**
     * Xử lý accept rematch
     */
    private void handleAcceptRematch(Message message) {
        if (!isAuthenticated()) return;
        
        server.getGameManager().handleAcceptRematch(user);
    }

    /**
     * Xử lý reject rematch
     */
    private void handleRejectRematch(Message message) {
        if (!isAuthenticated()) return;
        
        server.getGameManager().handleRejectRematch(user);
    }

    /**
     * Xử lý heartbeat
     */
    private void handleHeartbeat(Message message) {
        // Gửi heartbeat response
        sendMessage(Message.response(Message.Actions.HEARTBEAT, null, true));
    }

    /**
     * Kiểm tra user đã authentication chưa
     */
    private boolean isAuthenticated() {
        if (user == null) {
            sendErrorMessage("auth_required", "Bạn cần đăng nhập trước");
            return false;
        }
        return true;
    }

    /**
     * Gửi message đến client
     */
    public boolean sendMessage(Message message) {
        if (!connected.get() || writer == null) {
            return false;
        }
        
        try {
            String json = message.toJson();
            writer.println(json);
            return !writer.checkError();
        } catch (Exception e) {
            logger.error("Lỗi gửi message đến client {}: {}", getClientAddress(), e.getMessage());
            return false;
        }
    }

    /**
     * Gửi error message
     */
    private void sendErrorMessage(String action, String errorMessage) {
        sendMessage(Message.error(action, errorMessage));
    }

    /**
     * Ngắt kết nối client
     */
    public void disconnect() {
        if (connected.getAndSet(false)) {
            try {
                // Unregister từ server
                server.unregisterClient(this);
                
                // Đóng streams
                if (reader != null) reader.close();
                if (writer != null) writer.close();
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
                
            } catch (IOException e) {
                logger.error("Lỗi đóng connection: {}", e.getMessage());
            }
            
            logger.info("Client {} đã disconnect", getClientAddress());
        }
    }

    /**
     * Lấy địa chỉ client
     */
    public String getClientAddress() {
        if (clientSocket != null) {
            return clientSocket.getRemoteSocketAddress().toString();
        }
        return "unknown";
    }

    /**
     * Kiểm tra connection còn active không
     */
    public boolean isConnected() {
        return connected.get() && clientSocket != null && !clientSocket.isClosed();
    }

    /**
     * Lấy user hiện tại
     */
    public User getUser() {
        return user;
    }
}
