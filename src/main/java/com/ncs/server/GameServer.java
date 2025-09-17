package com.ncs.server;

import com.ncs.dao.DatabaseConnection;
import com.ncs.dao.GameDAO;
import com.ncs.dao.UserDAO;
import com.ncs.model.User;
import com.ncs.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main server class cho Color Guessing Game
 * Quản lý kết nối client và điều phối các game session
 */
public class GameServer {
    private static final Logger logger = LoggerFactory.getLogger(GameServer.class);
    
    private static final int DEFAULT_PORT = 8888;
    private final int port;
    private ServerSocket serverSocket;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService clientThreadPool;
    
    // Quản lý client connections
    private final ConcurrentHashMap<Integer, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ClientHandler> clientsByUsername = new ConcurrentHashMap<>();
    
    // Game management
    private final GameManager gameManager;
    
    // DAO instances
    private final UserDAO userDAO;
    private final GameDAO gameDAO;

    public GameServer() {
        this(DEFAULT_PORT);
    }

    public GameServer(int port) {
        this.port = port;
        this.userDAO = new UserDAO();
        this.gameDAO = new GameDAO();
        this.gameManager = new GameManager(this, userDAO, gameDAO);
        this.clientThreadPool = Executors.newCachedThreadPool();
    }

    /**
     * Khởi động server
     */
    public void start() {
        try {
            // Kiểm tra kết nối database
            if (!DatabaseConnection.getInstance().testConnection()) {
                logger.error("Không thể kết nối đến database. Server không thể khởi động.");
                return;
            }
            
            serverSocket = new ServerSocket(port);
            running.set(true);
            
            logger.info("Color Guessing Game Server đã khởi động trên port {}", port);
            logger.info("Đang chờ client kết nối...");
            
            // Khởi động game manager
            gameManager.start();
            
            // Accept client connections
            while (running.get()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    logger.info("Client mới kết nối từ: {}", clientSocket.getRemoteSocketAddress());
                    
                    // Tạo ClientHandler trong thread pool
                    clientThreadPool.submit(() -> {
                        ClientHandler clientHandler = new ClientHandler(this, clientSocket, userDAO, gameDAO);
                        clientHandler.start();
                    });
                    
                } catch (IOException e) {
                    if (running.get()) {
                        logger.error("Lỗi accept client connection: {}", e.getMessage());
                    }
                }
            }
            
        } catch (IOException e) {
            logger.error("Lỗi khởi động server: {}", e.getMessage(), e);
        } finally {
            stop();
        }
    }

    /**
     * Dừng server
     */
    public void stop() {
        logger.info("Đang dừng server...");
        running.set(false);
        
        try {
            // Đóng tất cả client connections
            for (ClientHandler client : connectedClients.values()) {
                client.disconnect();
            }
            connectedClients.clear();
            clientsByUsername.clear();
            
            // Dừng game manager
            if (gameManager != null) {
                gameManager.stop();
            }
            
            // Đóng thread pool
            if (clientThreadPool != null && !clientThreadPool.isShutdown()) {
                clientThreadPool.shutdown();
            }
            
            // Đóng server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // Đóng database connection
            DatabaseConnection.getInstance().closeConnection();
            
            logger.info("Server đã dừng");
            
        } catch (IOException e) {
            logger.error("Lỗi khi dừng server: {}", e.getMessage());
        }
    }

    /**
     * Đăng ký client handler khi user login thành công
     */
    public void registerClient(ClientHandler clientHandler, User user) {
        connectedClients.put(user.getId(), clientHandler);
        clientsByUsername.put(user.getUsername(), clientHandler);
        
        // Cập nhật trạng thái user trong database
        userDAO.updateUserStatus(user.getId(), User.UserStatus.ONLINE, true);
        
        logger.info("User {} đã đăng nhập từ {}", user.getUsername(), 
                   clientHandler.getClientAddress());
        
        // Thông báo cho các client khác về user mới online
        broadcastUserStatusUpdate(user, "joined");
        
        // Gửi danh sách users online cho client mới
        sendOnlineUsersToClient(clientHandler);
    }

    /**
     * Hủy đăng ký client khi disconnect
     */
    public void unregisterClient(ClientHandler clientHandler) {
        User user = clientHandler.getUser();
        if (user != null) {
            connectedClients.remove(user.getId());
            clientsByUsername.remove(user.getUsername());
            
            // Cập nhật trạng thái user trong database
            userDAO.updateUserStatus(user.getId(), User.UserStatus.OFFLINE, false);
            
            logger.info("User {} đã ngắt kết nối", user.getUsername());
            
            // Thông báo cho các client khác
            broadcastUserStatusUpdate(user, "left");
            
            // Xử lý nếu user đang trong game
            gameManager.handleUserDisconnect(user);
        }
    }

    /**
     * Gửi message đến một user cụ thể
     */
    public boolean sendMessageToUser(int userId, Message message) {
        ClientHandler client = connectedClients.get(userId);
        if (client != null) {
            return client.sendMessage(message);
        }
        return false;
    }

    /**
     * Gửi message đến user theo username
     */
    public boolean sendMessageToUser(String username, Message message) {
        ClientHandler client = clientsByUsername.get(username);
        if (client != null) {
            return client.sendMessage(message);
        }
        return false;
    }

    /**
     * Broadcast message đến tất cả connected clients
     */
    public void broadcastMessage(Message message) {
        for (ClientHandler client : connectedClients.values()) {
            client.sendMessage(message);
        }
    }

    /**
     * Broadcast message đến tất cả clients trừ sender
     */
    public void broadcastMessage(Message message, ClientHandler excludeClient) {
        for (ClientHandler client : connectedClients.values()) {
            if (client != excludeClient) {
                client.sendMessage(message);
            }
        }
    }

    /**
     * Gửi thông báo thay đổi trạng thái user
     */
    private void broadcastUserStatusUpdate(User user, String action) {
        Message notification = Message.notification(Message.Actions.USER_STATUS_CHANGED, null);
        notification.putData("user", user);
        notification.putData("action", action);
        
        broadcastMessage(notification);
    }

    /**
     * Gửi danh sách users online cho client
     */
    private void sendOnlineUsersToClient(ClientHandler clientHandler) {
        java.util.List<User> onlineUsers = userDAO.getOnlineUsers();
        
        Message response = Message.response(Message.Actions.GET_ONLINE_USERS, null, true);
        response.putData("users", onlineUsers);
        
        clientHandler.sendMessage(response);
    }

    /**
     * Kiểm tra user có online không
     */
    public boolean isUserOnline(int userId) {
        return connectedClients.containsKey(userId);
    }

    /**
     * Kiểm tra user có online không theo username
     */
    public boolean isUserOnline(String username) {
        return clientsByUsername.containsKey(username);
    }

    /**
     * Lấy số lượng client đang kết nối
     */
    public int getConnectedClientCount() {
        return connectedClients.size();
    }

    /**
     * Lấy GameManager instance
     */
    public GameManager getGameManager() {
        return gameManager;
    }

    /**
     * Kiểm tra server có đang chạy không
     */
    public boolean isRunning() {
        return running.get();
    }

    /**
     * Lấy port server
     */
    public int getPort() {
        return port;
    }

    /**
     * Main method để chạy server
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // Parse command line arguments
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65535) {
                    logger.warn("Port {} không hợp lệ, sử dụng port mặc định {}", port, DEFAULT_PORT);
                    port = DEFAULT_PORT;
                }
            } catch (NumberFormatException e) {
                logger.warn("Port {} không hợp lệ, sử dụng port mặc định {}", args[0], DEFAULT_PORT);
                port = DEFAULT_PORT;
            }
        }
        
        // Khởi tạo và chạy server
        GameServer server = new GameServer(port);
        
        // Xử lý shutdown gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Nhận tín hiệu shutdown, đang dừng server...");
            server.stop();
        }));
        
        // Chạy server
        server.start();
    }
}

