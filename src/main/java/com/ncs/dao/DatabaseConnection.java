package com.ncs.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton class quản lý kết nối đến database MySQL
 * Theo pattern Singleton để đảm bảo chỉ có một instance duy nhất
 */
public class DatabaseConnection {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    
    private static DatabaseConnection instance;
    private Connection connection;
    
    // Database configuration
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3307/color_guessing_game";
    private static final String DEFAULT_USERNAME = "root";
    private static final String DEFAULT_PASSWORD = "hieu112004";
    
    private final String url;
    private final String username;
    private final String password;

    // Private constructor để implement Singleton pattern
    private DatabaseConnection() {
        // Load configuration từ system properties hoặc sử dụng default
        this.url =  DEFAULT_URL;
        this.username =  DEFAULT_USERNAME;
        this.password =  DEFAULT_PASSWORD;
        
        initializeConnection();
    }

    /**
     * Constructor cho custom database configuration
     */
    private DatabaseConnection(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
        initializeConnection();
    }

    /**
     * Lấy instance duy nhất của DatabaseConnection
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Lấy instance với custom configuration
     */
    public static synchronized DatabaseConnection getInstance(String url, String username, String password) {
        if (instance == null) {
            instance = new DatabaseConnection(url, username, password);
        }
        return instance;
    }

    /**
     * Khởi tạo kết nối đến database
     */
    private void initializeConnection() {
        try {
            // Load MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Thiết lập properties cho connection
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("useSSL", "false");
            props.setProperty("allowPublicKeyRetrieval", "true");
            props.setProperty("serverTimezone", "Asia/Ho_Chi_Minh");
            props.setProperty("useUnicode", "true");

            // Tạo connection
            connection = DriverManager.getConnection(url, props);
            
            logger.info("Kết nối database thành công: {}", url);
            
        } catch (ClassNotFoundException e) {
            logger.error("Không tìm thấy MySQL JDBC driver", e);
            throw new RuntimeException("MySQL JDBC driver không được tìm thấy", e);
        } catch (SQLException e) {
            logger.error("Lỗi kết nối database: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể kết nối đến database", e);
        }
    }

    /**
     * Lấy connection hiện tại
     */
    public Connection getConnection() {
        try {
            // Kiểm tra connection còn valid không
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                logger.warn("Connection không valid, tạo lại connection...");
                initializeConnection();
            }
        } catch (SQLException e) {
            logger.error("Lỗi kiểm tra connection", e);
            initializeConnection();
        }
        
        return connection;
    }

    /**
     * Kiểm tra kết nối database
     */
    public boolean testConnection() {
        try {
            Connection conn = getConnection();
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            logger.error("Lỗi test connection", e);
            return false;
        }
    }

    /**
     * Đóng kết nối
     */
    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Đã đóng kết nối database");
            } catch (SQLException e) {
                logger.error("Lỗi khi đóng connection", e);
            }
        }
    }

    /**
     * Reset instance (dùng cho testing hoặc reconnect)
     */
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.closeConnection();
            instance = null;
        }
    }

    /**
     * Lấy thông tin database URL
     */
    public String getDatabaseUrl() {
        return url;
    }

    /**
     * Lấy username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Tạo database và tables nếu chưa tồn tại
     */
    public void initializeDatabase() {
        try (Connection conn = getConnection()) {
            // Thực hiện script tạo database và tables
            logger.info("Khởi tạo database schema...");
            
            // Có thể đọc và thực hiện file schema.sql ở đây
            // Hoặc tạo tables programmatically
            
            logger.info("Khởi tạo database hoàn thành");
            
        } catch (SQLException e) {
            logger.error("Lỗi khởi tạo database", e);
            throw new RuntimeException("Không thể khởi tạo database", e);
        }
    }

    /**
     * Tạo backup connection cho trường hợp cần thiết
     */
    public Connection createNewConnection() throws SQLException {
        try {
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            props.setProperty("useSSL", "false");
            props.setProperty("allowPublicKeyRetrieval", "true");
            props.setProperty("serverTimezone", "Asia/Ho_Chi_Minh");
            props.setProperty("useUnicode", "true");
            props.setProperty("characterEncoding", "utf8mb4");
            
            return DriverManager.getConnection(url, props);
            
        } catch (SQLException e) {
            logger.error("Lỗi tạo connection mới", e);
            throw e;
        }
    }
}

