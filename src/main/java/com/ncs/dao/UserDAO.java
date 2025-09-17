package com.ncs.dao;

import com.ncs.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class cho User entity
 * Xử lý các thao tác CRUD với bảng users
 */
public class UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
    private final DatabaseConnection dbConnection;

    public UserDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
    }

    /**
     * Tạo user mới
     */
    public boolean createUser(User user) {
        String sql = "INSERT INTO users (username, password, email, full_name, ranking_points, total_games, wins, losses, draws) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPassword());
            pstmt.setString(3, user.getEmail());
            pstmt.setString(4, user.getFullName());
            pstmt.setInt(5, user.getRankingPoints());
            pstmt.setInt(6, user.getTotalGames());
            pstmt.setInt(7, user.getWins());
            pstmt.setInt(8, user.getLosses());
            pstmt.setInt(9, user.getDraws());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                // Lấy ID được tạo tự động
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                logger.info("Tạo user thành công: {}", user.getUsername());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi tạo user: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Tìm user theo username
     */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi tìm user theo username: {}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Tìm user theo ID
     */
    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi tìm user theo ID: {}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Xác thực user (login)
     */
    public User authenticateUser(String username, String password) {
        User user = findByUsername(username);
        
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        
        return null;
    }

    /**
     * Cập nhật thông tin user
     */
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET email = ?, full_name = ?, ranking_points = ?, " +
                    "total_games = ?, wins = ?, losses = ?, draws = ?, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getFullName());
            pstmt.setInt(3, user.getRankingPoints());
            pstmt.setInt(4, user.getTotalGames());
            pstmt.setInt(5, user.getWins());
            pstmt.setInt(6, user.getLosses());
            pstmt.setInt(7, user.getDraws());
            pstmt.setInt(8, user.getId());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Cập nhật user thành công: {}", user.getUsername());
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật user: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Cập nhật trạng thái online của user
     */
    public boolean updateUserStatus(int userId, User.UserStatus status, boolean isOnline) {
        String sql = "UPDATE users SET status = ?, is_online = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status.toString().toLowerCase());
            pstmt.setBoolean(2, isOnline);
            pstmt.setInt(3, userId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật trạng thái user: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Lấy danh sách users online
     */
    public List<User> getOnlineUsers() {
        String sql = "SELECT * FROM users WHERE is_online = true ORDER BY ranking_points DESC";
        List<User> users = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi lấy danh sách users online: {}", e.getMessage(), e);
        }
        
        return users;
    }

    /**
     * Lấy bảng xếp hạng (top users theo điểm)
     */
    public List<User> getLeaderboard(int limit) {
        String sql = "SELECT * FROM users ORDER BY ranking_points DESC, total_games DESC, wins DESC LIMIT ?";
        List<User> users = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, limit);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi lấy bảng xếp hạng: {}", e.getMessage(), e);
        }
        
        return users;
    }

    /**
     * Cập nhật điểm ranking sau trận đấu
     */
    public boolean updateRankingPoints(int userId, int pointsChange) {
        String sql = "UPDATE users SET ranking_points = GREATEST(0, ranking_points + ?), " +
                    "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, pointsChange);
            pstmt.setInt(2, userId);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                logger.info("Cập nhật điểm ranking cho user {}: {} điểm", userId, pointsChange);
                return true;
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật điểm ranking: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Cập nhật thống kê sau trận đấu
     */
    public boolean updateGameStats(int userId, boolean isWin, boolean isDraw) {
        String sql = "UPDATE users SET " +
                    "total_games = total_games + 1, " +
                    "wins = wins + ?, " +
                    "draws = draws + ?, " +
                    "losses = losses + ?, " +
                    "updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, isWin ? 1 : 0);
            pstmt.setInt(2, isDraw ? 1 : 0);
            pstmt.setInt(3, (!isWin && !isDraw) ? 1 : 0);
            pstmt.setInt(4, userId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật thống kê game: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Kiểm tra username đã tồn tại chưa
     */
    public boolean isUsernameExists(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi kiểm tra username tồn tại: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Kiểm tra email đã tồn tại chưa
     */
    public boolean isEmailExists(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, email);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi kiểm tra email tồn tại: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Map ResultSet thành User object
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password"));
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setRankingPoints(rs.getInt("ranking_points"));
        user.setTotalGames(rs.getInt("total_games"));
        user.setWins(rs.getInt("wins"));
        user.setLosses(rs.getInt("losses"));
        user.setDraws(rs.getInt("draws"));
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            user.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        user.setOnline(rs.getBoolean("is_online"));
        
        String status = rs.getString("status");
        if (status != null) {
            try {
                user.setStatus(User.UserStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                user.setStatus(User.UserStatus.OFFLINE);
            }
        }
        
        return user;
    }
}
