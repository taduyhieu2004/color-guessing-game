package com.ncs.client;

import com.ncs.model.User;
import com.ncs.protocol.Message;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Client class xử lý kết nối và communication với server
 * Theo pattern Observer để notify UI về các events
 */
public class GameClient {
    private static final Logger logger = LoggerFactory.getLogger(GameClient.class);
    
    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8888;
    
    private String serverHost;
    private int serverPort;
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final AtomicBoolean connected = new AtomicBoolean(false);
    
    // Current user
    private User currentUser;
    
    // Message handlers
    private MessageHandler messageHandler;
    
    // Callbacks for UI
    private Runnable onConnectionLost;
    private Consumer<String> onError;

    public GameClient() {
        this(DEFAULT_HOST, DEFAULT_PORT);
    }

    public GameClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
        this.messageHandler = new MessageHandler(this);
    }

    /**
     * Kết nối đến server
     */
    public boolean connect() {
        if (connected.get()) {
            return true;
        }
        
        try {
            logger.info("Đang kết nối đến server {}:{}", serverHost, serverPort);
            
            socket = new Socket(serverHost, serverPort);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            
            connected.set(true);
            
            // Bắt đầu thread đọc messages từ server
            startMessageReader();
            
            logger.info("Kết nối đến server thành công");
            return true;
            
        } catch (IOException e) {
            logger.error("Lỗi kết nối đến server: {}", e.getMessage());
            if (onError != null) { SwingUtilities.invokeLater(() -> onError.accept("Không thể kết nối đến server: " + e.getMessage())); }
            return false;
        }
    }

    /**
     * Ngắt kết nối
     */
    public void disconnect() {
        if (!connected.get()) {
            return;
        }
        
        connected.set(false);
        currentUser = null;
        
        try {
            if (writer != null) {
                // Gửi logout message trước khi đóng
                if (currentUser != null) {
                    sendMessage(Message.request(Message.Actions.LOGOUT, null));
                }
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            
            logger.info("Đã ngắt kết nối khỏi server");
            
        } catch (IOException e) {
            logger.error("Lỗi khi đóng kết nối: {}", e.getMessage());
        }
    }

    /**
     * Gửi message đến server
     */
    public boolean sendMessage(Message message) {
        if (!connected.get() || writer == null) {
            logger.warn("Không thể gửi message: không có kết nối");
            return false;
        }
        
        try {
            String json = message.toJson();
            writer.println(json);
            
            logger.debug("Sent message: {}", message.getAction());
            return !writer.checkError();
            
        } catch (Exception e) {
            logger.error("Lỗi gửi message: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Đăng nhập
     */
    public void login(String username, String password) {
        if (!connected.get()) {
            if (!connect()) {
                return;
            }
        }
        
        Message loginMessage = Message.request(Message.Actions.LOGIN, null);
        loginMessage.putData("username", username);
        loginMessage.putData("password", password);
        
        sendMessage(loginMessage);
    }

    /**
     * Đăng ký
     */
    public void register(String username, String password, String email, String fullName) {
        if (!connected.get()) {
            if (!connect()) {
                return;
            }
        }
        
        Message registerMessage = Message.request(Message.Actions.REGISTER, null);
        registerMessage.putData("username", username);
        registerMessage.putData("password", password);
        if (email != null && !email.trim().isEmpty()) {
            registerMessage.putData("email", email);
        }
        if (fullName != null && !fullName.trim().isEmpty()) {
            registerMessage.putData("fullName", fullName);
        }
        
        sendMessage(registerMessage);
    }

    /**
     * Đăng xuất
     */
    public void logout() {
        if (connected.get() && currentUser != null) {
            sendMessage(Message.request(Message.Actions.LOGOUT, null));
        }
        currentUser = null;
    }

    /**
     * Lấy danh sách users online
     */
    public void getOnlineUsers() {
        if (connected.get()) {
            sendMessage(Message.request(Message.Actions.GET_ONLINE_USERS, null));
        }
    }

    /**
     * Lấy bảng xếp hạng
     */
    public void getLeaderboard(int limit) {
        if (connected.get()) {
            Message message = Message.request(Message.Actions.GET_LEADERBOARD, null);
            message.putData("limit", limit);
            sendMessage(message);
        }
    }

    /**
     * Gửi lời mời thách đấu
     */
    public void sendInvitation(String targetUsername, String message) {
        if (connected.get()) {
            Message inviteMessage = Message.request(Message.Actions.SEND_INVITATION, null);
            inviteMessage.putData("targetUsername", targetUsername);
            if (message != null && !message.trim().isEmpty()) {
                inviteMessage.putData("message", message);
            }
            sendMessage(inviteMessage);
        }
    }

    /**
     * Chấp nhận lời mời
     */
    public void acceptInvitation(int invitationId) {
        if (connected.get()) {
            Message message = Message.request(Message.Actions.ACCEPT_INVITATION, null);
            message.putData("invitationId", invitationId);
            sendMessage(message);
        }
    }

    /**
     * Từ chối lời mời
     */
    public void rejectInvitation(int invitationId) {
        if (connected.get()) {
            Message message = Message.request(Message.Actions.REJECT_INVITATION, null);
            message.putData("invitationId", invitationId);
            sendMessage(message);
        }
    }

    /**
     * Submit guess trong game
     */
    public void submitGuess(java.util.List<com.ncs.model.GameColor> colors, int timeMillis) {
        if (connected.get()) {
            Message message = Message.request(Message.Actions.SUBMIT_GUESS, null);
            message.putData("colors", colors);
            message.putData("timeMillis", timeMillis);
            sendMessage(message);
        }
    }

    /**
     * Rời khỏi game
     */
    public void leaveGame() {
        if (connected.get()) {
            sendMessage(Message.request(Message.Actions.LEAVE_GAME, null));
        }
    }

    /**
     * Yêu cầu rematch
     */
    public void requestRematch() {
        if (connected.get()) {
            sendMessage(Message.request(Message.Actions.REQUEST_REMATCH, null));
        }
    }

    /**
     * Chấp nhận rematch
     */
    public void acceptRematch() {
        if (connected.get()) {
            sendMessage(Message.request(Message.Actions.ACCEPT_REMATCH, null));
        }
    }

    /**
     * Từ chối rematch
     */
    public void rejectRematch() {
        if (connected.get()) {
            sendMessage(Message.request(Message.Actions.REJECT_REMATCH, null));
        }
    }

    /**
     * Bắt đầu thread đọc messages từ server
     */
    private void startMessageReader() {
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while (connected.get() && (line = reader.readLine()) != null) {
                    try {
                        Message message = Message.fromJson(line);
                        logger.debug("Received message: {}", message.getAction());
                        
                        // Xử lý message ngay trên reader thread; các UI listener sẽ tự
                        // đẩy cập nhật UI lên EDT khi cần.
                        messageHandler.handleMessage(message);
                        
                    } catch (Exception e) {
                        logger.error("Lỗi xử lý message: {}", e.getMessage());
                    }
                }
            } catch (SocketException e) {
                logger.info("Socket connection closed");
            } catch (IOException e) {
                logger.error("Lỗi đọc message từ server: {}", e.getMessage());
                
                if (connected.get()) { SwingUtilities.invokeLater(() -> { if (onConnectionLost != null) { onConnectionLost.run(); } }); }
            } finally {
                connected.set(false);
            }
        });
        
        readerThread.setDaemon(true);
        readerThread.setName("GameClient-MessageReader");
        readerThread.start();
    }

    // Getters and Setters

    public boolean isConnected() {
        return connected.get();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public String getServerHost() {
        return serverHost;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    // Callback setters

    public void setOnConnectionLost(Runnable onConnectionLost) {
        this.onConnectionLost = onConnectionLost;
    }

    public void setOnError(Consumer<String> onError) {
        this.onError = onError;
    }

    /**
     * Send heartbeat to server
     */
    public void sendHeartbeat() {
        if (connected.get()) {
            sendMessage(Message.request(Message.Actions.HEARTBEAT, null));
        }
    }
}
