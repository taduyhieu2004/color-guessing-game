-- Database schema cho Color Guessing Game
-- Tạo database
CREATE DATABASE IF NOT EXISTS color_guessing_game CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE color_guessing_game;

-- Bảng users - lưu thông tin người chơi
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) UNIQUE,
    full_name VARCHAR(100),
    ranking_points INT DEFAULT 0,
    total_games INT DEFAULT 0,
    wins INT DEFAULT 0,
    losses INT DEFAULT 0,
    draws INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_online BOOLEAN DEFAULT FALSE,
    status ENUM('online', 'offline', 'playing') DEFAULT 'offline',
    
    INDEX idx_username (username),
    INDEX idx_ranking_points (ranking_points DESC),
    INDEX idx_is_online (is_online)
);

-- Bảng games - lưu thông tin các trận đấu
CREATE TABLE IF NOT EXISTS games (
    id INT AUTO_INCREMENT PRIMARY KEY,
    player1_id INT NOT NULL,
    player2_id INT NOT NULL,
    player1_score DECIMAL(3,1) DEFAULT 0,
    player2_score DECIMAL(3,1) DEFAULT 0,
    winner_id INT NULL,
    game_status ENUM('waiting', 'playing', 'finished', 'abandoned') DEFAULT 'waiting',
    current_round INT DEFAULT 0,
    max_rounds INT DEFAULT 5,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (player1_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (player2_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (winner_id) REFERENCES users(id) ON DELETE SET NULL,
    
    INDEX idx_players (player1_id, player2_id),
    INDEX idx_game_status (game_status),
    INDEX idx_created_at (created_at DESC)
);

-- Bảng game_rounds - lưu thông tin từng lượt chơi
CREATE TABLE IF NOT EXISTS game_rounds (
    id INT AUTO_INCREMENT PRIMARY KEY,
    game_id INT NOT NULL,
    round_number INT NOT NULL,
    target_colors JSON NOT NULL, -- Lưu 3 màu cần đoán dưới dạng JSON [{"r":255,"g":0,"b":0}, ...]
    player1_guess JSON NULL, -- Lưu lựa chọn của player 1
    player2_guess JSON NULL, -- Lưu lựa chọn của player 2
    player1_time INT NULL, -- Thời gian hoàn thành (milliseconds)
    player2_time INT NULL, -- Thời gian hoàn thành (milliseconds)
    player1_correct BOOLEAN DEFAULT FALSE,
    player2_correct BOOLEAN DEFAULT FALSE,
    round_winner_id INT NULL, -- Người thắng lượt này
    round_status ENUM('waiting', 'showing_colors', 'guessing', 'finished') DEFAULT 'waiting',
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE CASCADE,
    FOREIGN KEY (round_winner_id) REFERENCES users(id) ON DELETE SET NULL,
    
    UNIQUE KEY unique_game_round (game_id, round_number),
    INDEX idx_game_id (game_id),
    INDEX idx_round_status (round_status)
);

-- Bảng user_sessions - lưu thông tin phiên đăng nhập
CREATE TABLE IF NOT EXISTS user_sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    session_token VARCHAR(255) UNIQUE NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_session_token (session_token),
    INDEX idx_is_active (is_active)
);

-- Bảng game_invitations - lưu thông tin lời mời thách đấu
CREATE TABLE IF NOT EXISTS game_invitations (
    id INT AUTO_INCREMENT PRIMARY KEY,
    inviter_id INT NOT NULL,
    invitee_id INT NOT NULL,
    game_id INT NULL,
    status ENUM('pending', 'accepted', 'rejected', 'expired') DEFAULT 'pending',
    message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    responded_at TIMESTAMP NULL,
    expires_at TIMESTAMP NULL,
    
    FOREIGN KEY (inviter_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (invitee_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE SET NULL,
    
    INDEX idx_inviter_id (inviter_id),
    INDEX idx_invitee_id (invitee_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at DESC)
);

-- Bảng ranking_history - lưu lịch sử thay đổi điểm xếp hạng
CREATE TABLE IF NOT EXISTS ranking_history (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    game_id INT NULL,
    old_points INT NOT NULL,
    new_points INT NOT NULL,
    points_change INT NOT NULL,
    reason ENUM('win', 'loss', 'draw', 'abandon_win', 'abandon_loss') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (game_id) REFERENCES games(id) ON DELETE SET NULL,
    
    INDEX idx_user_id (user_id),
    INDEX idx_game_id (game_id),
    INDEX idx_created_at (created_at DESC)
);

-- Tạo một số user mẫu để test
INSERT INTO users (username, password, full_name, email, ranking_points) VALUES
('admin', 'admin123', 'Administrator', 'admin@colorguessing.com', 1000),
('player1', 'password1', 'Nguyễn Văn A', 'player1@example.com', 800),
('player2', 'password2', 'Trần Thị B', 'player2@example.com', 750),
('player3', 'password3', 'Lê Văn C', 'player3@example.com', 600),
('player4', 'password4', 'Phạm Thị D', 'player4@example.com', 500);

-- Cấp quyền cho user (điều chỉnh theo cấu hình MySQL của bạn)
-- GRANT ALL PRIVILEGES ON color_guessing_game.* TO 'gameuser'@'localhost' IDENTIFIED BY 'gamepassword';
-- FLUSH PRIVILEGES;

