package com.ncs.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncs.model.Game;
import com.ncs.model.GameColor;
import com.ncs.model.GameRound;
import com.ncs.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO class cho Game entity
 * Xử lý các thao tác CRUD với bảng games và game_rounds
 */
public class GameDAO {
    private static final Logger logger = LoggerFactory.getLogger(GameDAO.class);
    private final DatabaseConnection dbConnection;
    private final ObjectMapper objectMapper;
    private final UserDAO userDAO;

    public GameDAO() {
        this.dbConnection = DatabaseConnection.getInstance();
        this.objectMapper = new ObjectMapper();
        this.userDAO = new UserDAO();
    }

    /**
     * Tạo game mới
     */
    public Game createGame(int player1Id, int player2Id) {
        String sql = "INSERT INTO games (player1_id, player2_id, player1_score, player2_score, " +
                    "game_status, current_round, max_rounds) VALUES (?, ?, 0, 0, 'waiting', 0, 5)";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, player1Id);
            pstmt.setInt(2, player2Id);
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int gameId = generatedKeys.getInt(1);
                        Game game = new Game(player1Id, player2Id);
                        game.setId(gameId);
                        
                        // Load thông tin players
                        game.setPlayer1(userDAO.findById(player1Id));
                        game.setPlayer2(userDAO.findById(player2Id));
                        
                        logger.info("Tạo game thành công: {} vs {}", player1Id, player2Id);
                        return game;
                    }
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi tạo game: {}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Tìm game theo ID
     */
    public Game findById(int gameId) {
        String sql = "SELECT * FROM games WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, gameId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Game game = mapResultSetToGame(rs);
                    // Load rounds
                    game.setRounds(getGameRounds(gameId));
                    return game;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi tìm game theo ID: {}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Tìm game đang chơi của user
     */
    public Game findActiveGameByUserId(int userId) {
        String sql = "SELECT * FROM games WHERE (player1_id = ? OR player2_id = ?) " +
                    "AND game_status IN ('waiting', 'playing') ORDER BY created_at DESC LIMIT 1";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Game game = mapResultSetToGame(rs);
                    game.setRounds(getGameRounds(game.getId()));
                    return game;
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi tìm game đang chơi: {}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Cập nhật trạng thái game
     */
    public boolean updateGameStatus(int gameId, Game.GameStatus status) {
        String sql = "UPDATE games SET game_status = ?, " +
                    (status == Game.GameStatus.PLAYING ? "started_at = CURRENT_TIMESTAMP, " : "") +
                    (status == Game.GameStatus.FINISHED || status == Game.GameStatus.ABANDONED ? 
                     "finished_at = CURRENT_TIMESTAMP, " : "") +
                    "current_round = current_round WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status.toString().toLowerCase());
            pstmt.setInt(2, gameId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật trạng thái game: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Cập nhật điểm số game
     */
    public boolean updateGameScore(int gameId, double player1Score, double player2Score, int currentRound) {
        String sql = "UPDATE games SET player1_score = ?, player2_score = ?, current_round = ? WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, player1Score);
            pstmt.setDouble(2, player2Score);
            pstmt.setInt(3, currentRound);
            pstmt.setInt(4, gameId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật điểm game: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Kết thúc game và xác định winner
     */
    public boolean finishGame(int gameId, Integer winnerId) {
        String sql = "UPDATE games SET game_status = 'finished', winner_id = ?, " +
                    "finished_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (winnerId != null) {
                pstmt.setInt(1, winnerId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, gameId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            logger.error("Lỗi kết thúc game: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Tạo round mới cho game
     */
    public GameRound createGameRound(int gameId, int roundNumber, List<GameColor> targetColors) {
        String sql = "INSERT INTO game_rounds (game_id, round_number, target_colors, round_status) " +
                    "VALUES (?, ?, ?, 'waiting')";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setInt(1, gameId);
            pstmt.setInt(2, roundNumber);
            pstmt.setString(3, objectMapper.writeValueAsString(targetColors));
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int roundId = generatedKeys.getInt(1);
                        GameRound round = new GameRound(gameId, roundNumber, targetColors);
                        round.setId(roundId);
                        return round;
                    }
                }
            }
            
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Lỗi tạo game round: {}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Cập nhật guess của player cho round
     */
    public boolean updatePlayerGuess(int roundId, int playerId, List<GameColor> guess, int timeMillis) {
        // Xác định player nào (1 hoặc 2)
        Game game = getGameByRoundId(roundId);
        if (game == null) return false;
        
        String sql;
        if (game.getPlayer1Id() == playerId) {
            sql = "UPDATE game_rounds SET player1_guess = ?, player1_time = ?, player1_correct = ? WHERE id = ?";
        } else if (game.getPlayer2Id() == playerId) {
            sql = "UPDATE game_rounds SET player2_guess = ?, player2_time = ?, player2_correct = ? WHERE id = ?";
        } else {
            return false;
        }
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // Lấy target colors để kiểm tra kết quả
            GameRound round = findRoundById(roundId);
            boolean isCorrect = checkGuess(guess, round.getTargetColors());
            
            pstmt.setString(1, objectMapper.writeValueAsString(guess));
            pstmt.setInt(2, timeMillis);
            pstmt.setBoolean(3, isCorrect);
            pstmt.setInt(4, roundId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException | JsonProcessingException e) {
            logger.error("Lỗi cập nhật guess: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Cập nhật trạng thái round
     */
    public boolean updateRoundStatus(int roundId, GameRound.RoundStatus status) {
        String sql = "UPDATE game_rounds SET round_status = ?, " +
                    (status == GameRound.RoundStatus.SHOWING_COLORS ? "started_at = CURRENT_TIMESTAMP, " : "") +
                    (status == GameRound.RoundStatus.FINISHED ? "finished_at = CURRENT_TIMESTAMP, " : "") +
                    "id = id WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, status.toString().toLowerCase());
            pstmt.setInt(2, roundId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            logger.error("Lỗi cập nhật trạng thái round: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Kết thúc round và xác định winner
     */
    public boolean finishRound(int roundId, Integer winnerId) {
        String sql = "UPDATE game_rounds SET round_status = 'finished', round_winner_id = ?, " +
                    "finished_at = CURRENT_TIMESTAMP WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            if (winnerId != null) {
                pstmt.setInt(1, winnerId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, roundId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            logger.error("Lỗi kết thúc round: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Lấy danh sách rounds của game
     */
    public List<GameRound> getGameRounds(int gameId) {
        String sql = "SELECT * FROM game_rounds WHERE game_id = ? ORDER BY round_number";
        List<GameRound> rounds = new ArrayList<>();
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, gameId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    rounds.add(mapResultSetToGameRound(rs));
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi lấy game rounds: {}", e.getMessage(), e);
        }
        
        return rounds;
    }

    /**
     * Tìm round theo ID
     */
    public GameRound findRoundById(int roundId) {
        String sql = "SELECT * FROM game_rounds WHERE id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, roundId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToGameRound(rs);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi tìm round theo ID: {}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Lấy game từ round ID
     */
    private Game getGameByRoundId(int roundId) {
        String sql = "SELECT g.* FROM games g JOIN game_rounds gr ON g.id = gr.game_id WHERE gr.id = ?";
        
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, roundId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToGame(rs);
                }
            }
            
        } catch (SQLException e) {
            logger.error("Lỗi lấy game từ round ID: {}", e.getMessage(), e);
        }
        
        return null;
    }

    /**
     * Kiểm tra guess có đúng không
     */
    private boolean checkGuess(List<GameColor> guess, List<GameColor> targetColors) {
        if (guess == null || targetColors == null || guess.size() != targetColors.size()) {
            return false;
        }
        
        // Kiểm tra xem tất cả màu trong guess có khớp với targetColors không
        for (GameColor guessColor : guess) {
            boolean found = false;
            for (GameColor targetColor : targetColors) {
                if (guessColor.equals(targetColor)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Map ResultSet thành Game object
     */
    private Game mapResultSetToGame(ResultSet rs) throws SQLException {
        Game game = new Game();
        game.setId(rs.getInt("id"));
        game.setPlayer1Id(rs.getInt("player1_id"));
        game.setPlayer2Id(rs.getInt("player2_id"));
        game.setPlayer1Score(rs.getDouble("player1_score"));
        game.setPlayer2Score(rs.getDouble("player2_score"));
        
        int winnerId = rs.getInt("winner_id");
        if (!rs.wasNull()) {
            game.setWinnerId(winnerId);
        }
        
        String status = rs.getString("game_status");
        if (status != null) {
            try {
                game.setGameStatus(Game.GameStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                game.setGameStatus(Game.GameStatus.WAITING);
            }
        }
        
        game.setCurrentRound(rs.getInt("current_round"));
        game.setMaxRounds(rs.getInt("max_rounds"));
        
        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) {
            game.setStartedAt(startedAt.toLocalDateTime());
        }
        
        Timestamp finishedAt = rs.getTimestamp("finished_at");
        if (finishedAt != null) {
            game.setFinishedAt(finishedAt.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            game.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        // Load player information
        game.setPlayer1(userDAO.findById(game.getPlayer1Id()));
        game.setPlayer2(userDAO.findById(game.getPlayer2Id()));
        
        return game;
    }

    /**
     * Map ResultSet thành GameRound object
     */
    private GameRound mapResultSetToGameRound(ResultSet rs) throws SQLException {
        GameRound round = new GameRound();
        round.setId(rs.getInt("id"));
        round.setGameId(rs.getInt("game_id"));
        round.setRoundNumber(rs.getInt("round_number"));
        
        // Parse JSON colors
        try {
            String targetColorsJson = rs.getString("target_colors");
            if (targetColorsJson != null) {
                List<GameColor> targetColors = objectMapper.readValue(targetColorsJson, 
                    new TypeReference<List<GameColor>>() {});
                round.setTargetColors(targetColors);
            }
            
            String player1GuessJson = rs.getString("player1_guess");
            if (player1GuessJson != null) {
                List<GameColor> player1Guess = objectMapper.readValue(player1GuessJson, 
                    new TypeReference<List<GameColor>>() {});
                round.setPlayer1Guess(player1Guess);
            }
            
            String player2GuessJson = rs.getString("player2_guess");
            if (player2GuessJson != null) {
                List<GameColor> player2Guess = objectMapper.readValue(player2GuessJson, 
                    new TypeReference<List<GameColor>>() {});
                round.setPlayer2Guess(player2Guess);
            }
            
        } catch (JsonProcessingException e) {
            logger.error("Lỗi parse JSON colors: {}", e.getMessage(), e);
        }
        
        round.setPlayer1Time(rs.getInt("player1_time"));
        if (rs.wasNull()) round.setPlayer1Time(null);
        
        round.setPlayer2Time(rs.getInt("player2_time"));
        if (rs.wasNull()) round.setPlayer2Time(null);
        
        round.setPlayer1Correct(rs.getBoolean("player1_correct"));
        round.setPlayer2Correct(rs.getBoolean("player2_correct"));
        
        int roundWinnerId = rs.getInt("round_winner_id");
        if (!rs.wasNull()) {
            round.setRoundWinnerId(roundWinnerId);
        }
        
        String status = rs.getString("round_status");
        if (status != null) {
            try {
                round.setRoundStatus(GameRound.RoundStatus.valueOf(status.toUpperCase()));
            } catch (IllegalArgumentException e) {
                round.setRoundStatus(GameRound.RoundStatus.WAITING);
            }
        }
        
        Timestamp startedAt = rs.getTimestamp("started_at");
        if (startedAt != null) {
            round.setStartedAt(startedAt.toLocalDateTime());
        }
        
        Timestamp finishedAt = rs.getTimestamp("finished_at");
        if (finishedAt != null) {
            round.setFinishedAt(finishedAt.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            round.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        return round;
    }
}
