package com.ncs.server;

import com.ncs.model.*;
import com.ncs.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Quản lý một game session cụ thể
 * Xử lý game flow, rounds, scoring
 */
public class GameSession {
    private static final Logger logger = LoggerFactory.getLogger(GameSession.class);
    
    private final GameManager gameManager;
    private final Game game;
    private final AtomicBoolean running = new AtomicBoolean(false);
    
    // Current round state
    private GameRound currentRound;
    private final Map<Integer, PlayerGuess> currentGuesses = new HashMap<>();
    private CountDownLatch guessingLatch;
    
    // Game configuration
    private static final int SHOW_COLORS_DURATION = 5000; // 5 giây
    private static final int GUESSING_DURATION = 10000;   // 10 giây
    private static final int COLORS_COUNT = 3;            // 3 màu mỗi round
    
    // Available colors for the game
    private static final List<GameColor> AVAILABLE_COLORS = Arrays.asList(
        GameColor.RED, GameColor.GREEN, GameColor.BLUE, GameColor.YELLOW,
        GameColor.PURPLE, GameColor.ORANGE, GameColor.PINK, GameColor.CYAN,
        GameColor.BROWN, GameColor.GRAY, GameColor.BLACK, GameColor.WHITE
    );

    public GameSession(GameManager gameManager, Game game) {
        this.gameManager = gameManager;
        this.game = game;
    }

    /**
     * Bắt đầu game session
     */
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return; // Already running
        }
        
        logger.info("Bắt đầu game session {}: {} vs {}", 
                   game.getId(), game.getPlayer1().getUsername(), game.getPlayer2().getUsername());
        
        try {
            // Cập nhật trạng thái user
            updateUserStatus(User.UserStatus.PLAYING);
            
            // Bắt đầu game
            game.startGame();
            gameManager.gameDAO.updateGameStatus(game.getId(), Game.GameStatus.PLAYING);
            
            // Chạy 5 rounds
            for (int roundNum = 1; roundNum <= game.getMaxRounds(); roundNum++) {
                if (!running.get()) break;
                
                game.setCurrentRound(roundNum);
                runRound(roundNum);
                
                // Kiểm tra game có bị abandon không
                if (game.getGameStatus() == Game.GameStatus.ABANDONED) {
                    break;
                }
                
                // Nghỉ giữa các round
                if (roundNum < game.getMaxRounds()) {
                    Thread.sleep(2000);
                }
            }
            
            // Kết thúc game
            if (running.get() && game.getGameStatus() != Game.GameStatus.ABANDONED) {
                finishGame();
            }
            
        } catch (InterruptedException e) {
            logger.warn("Game session {} bị interrupt", game.getId());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error("Lỗi trong game session {}: {}", game.getId(), e.getMessage(), e);
        } finally {
            cleanup();
        }
    }

    /**
     * Chạy một round
     */
    private void runRound(int roundNumber) throws InterruptedException {
        logger.info("Bắt đầu round {} cho game {}", roundNumber, game.getId());
        
        // Tạo màu sắc ngẫu nhiên
        List<GameColor> targetColors = generateRandomColors();
        
        // Tạo round trong database
        currentRound = gameManager.gameDAO.createGameRound(game.getId(), roundNumber, targetColors);
        if (currentRound == null) {
            logger.error("Không thể tạo round {} cho game {}", roundNumber, game.getId());
            return;
        }
        
        // Reset guesses
        currentGuesses.clear();
        
        // Phase 1: Hiển thị màu sắc (5 giây)
        showColorsPhase();
        
        // Phase 2: Đoán màu (10 giây)
        guessingPhase();
        
        // Phase 3: Tính kết quả
        calculateRoundResult();
        
        // Cập nhật score game
        gameManager.gameDAO.updateGameScore(game.getId(), game.getPlayer1Score(), 
                                           game.getPlayer2Score(), game.getCurrentRound());
    }

    /**
     * Phase hiển thị màu sắc
     */
    private void showColorsPhase() throws InterruptedException {
        currentRound.setRoundStatus(GameRound.RoundStatus.SHOWING_COLORS);
        gameManager.gameDAO.updateRoundStatus(currentRound.getId(), GameRound.RoundStatus.SHOWING_COLORS);
        
        // Gửi màu sắc cho cả hai player
        Message showColorsMsg = Message.notification(Message.Actions.SHOW_COLORS, null);
        showColorsMsg.putData("colors", currentRound.getTargetColors());
        showColorsMsg.putData("duration", SHOW_COLORS_DURATION);
        showColorsMsg.putData("roundNumber", currentRound.getRoundNumber());
        
        sendMessageToPlayers(showColorsMsg);
        
        logger.info("Hiển thị màu cho round {} game {}: {}", 
                   currentRound.getRoundNumber(), game.getId(), currentRound.getTargetColors());
        
        // Chờ 5 giây
        Thread.sleep(SHOW_COLORS_DURATION);
    }

    /**
     * Phase đoán màu
     */
    private void guessingPhase() throws InterruptedException {
        currentRound.setRoundStatus(GameRound.RoundStatus.GUESSING);
        gameManager.gameDAO.updateRoundStatus(currentRound.getId(), GameRound.RoundStatus.GUESSING);
        
        // Tạo bảng màu với nhiều màu khác nhau
        List<GameColor> colorPalette = createColorPalette();
        
        // Gửi bảng màu và bắt đầu đếm thời gian
        Message startGuessingMsg = Message.notification(Message.Actions.START_GUESSING, null);
        startGuessingMsg.putData("colorPalette", colorPalette);
        startGuessingMsg.putData("duration", GUESSING_DURATION);
        startGuessingMsg.putData("roundNumber", currentRound.getRoundNumber());
        
        sendMessageToPlayers(startGuessingMsg);
        
        logger.info("Bắt đầu phase đoán màu cho round {} game {}", 
                   currentRound.getRoundNumber(), game.getId());
        
        // Chờ cả hai player guess hoặc timeout
        guessingLatch = new CountDownLatch(2);
        long startTime = System.currentTimeMillis();
        
        boolean completed = guessingLatch.await(GUESSING_DURATION, TimeUnit.MILLISECONDS);
        
        if (!completed) {
            logger.info("Timeout đoán màu cho round {} game {}", currentRound.getRoundNumber(), game.getId());
        }
        
        // Đảm bảo có guess cho cả hai player (empty guess nếu không submit)
        ensureAllPlayersHaveGuess(startTime);
    }

    /**
     * Tính kết quả round
     */
    private void calculateRoundResult() {
        PlayerGuess player1Guess = currentGuesses.get(game.getPlayer1Id());
        PlayerGuess player2Guess = currentGuesses.get(game.getPlayer2Id());
        
        // Cập nhật guess vào database
        if (player1Guess != null) {
            gameManager.gameDAO.updatePlayerGuess(currentRound.getId(), game.getPlayer1Id(), 
                                                 player1Guess.colors, player1Guess.timeMillis);
        }
        if (player2Guess != null) {
            gameManager.gameDAO.updatePlayerGuess(currentRound.getId(), game.getPlayer2Id(), 
                                                 player2Guess.colors, player2Guess.timeMillis);
        }
        
        // Xác định winner round
        Integer roundWinnerId = null;
        boolean player1Correct = player1Guess != null && checkGuess(player1Guess.colors);
        boolean player2Correct = player2Guess != null && checkGuess(player2Guess.colors);
        
        if (player1Correct && !player2Correct) {
            roundWinnerId = game.getPlayer1Id();
        } else if (!player1Correct && player2Correct) {
            roundWinnerId = game.getPlayer2Id();
        } else if (player1Correct && player2Correct) {
            // Cả hai đúng, so sánh thời gian
            if (player1Guess != null && player2Guess != null) {
                if (player1Guess.timeMillis < player2Guess.timeMillis) {
                    roundWinnerId = game.getPlayer1Id();
                } else if (player2Guess.timeMillis < player1Guess.timeMillis) {
                    roundWinnerId = game.getPlayer2Id();
                }
                // Nếu thời gian bằng nhau thì hòa (roundWinnerId = null)
            }
        }
        // Nếu cả hai sai thì không có winner (roundWinnerId = null)
        
        // Cập nhật điểm
        double player1Score = 0;
        double player2Score = 0;
        
        if (roundWinnerId == null) {
            // Hòa
            player1Score = 0.5;
            player2Score = 0.5;
        } else if (roundWinnerId == game.getPlayer1Id()) {
            // Player 1 thắng
            player1Score = 1.0;
            player2Score = 0.0;
        } else {
            // Player 2 thắng
            player1Score = 0.0;
            player2Score = 1.0;
        }
        
        game.addScore(game.getPlayer1Id(), player1Score);
        game.addScore(game.getPlayer2Id(), player2Score);
        
        // Kết thúc round
        currentRound.setRoundStatus(GameRound.RoundStatus.FINISHED);
        gameManager.gameDAO.finishRound(currentRound.getId(), roundWinnerId);
        
        // Gửi kết quả cho players
        Message resultMsg = Message.notification(Message.Actions.ROUND_RESULT, null);
        resultMsg.putData("roundNumber", currentRound.getRoundNumber());
        resultMsg.putData("targetColors", currentRound.getTargetColors());
        resultMsg.putData("player1Correct", player1Correct);
        resultMsg.putData("player2Correct", player2Correct);
        resultMsg.putData("winnerId", roundWinnerId);
        resultMsg.putData("player1Score", player1Score);
        resultMsg.putData("player2Score", player2Score);
        resultMsg.putData("totalScore1", game.getPlayer1Score());
        resultMsg.putData("totalScore2", game.getPlayer2Score());
        
        sendMessageToPlayers(resultMsg);
        
        logger.info("Kết quả round {} game {}: Winner={}, Score={}-{}", 
                   currentRound.getRoundNumber(), game.getId(), roundWinnerId, 
                   game.getPlayer1Score(), game.getPlayer2Score());
    }

    /**
     * Kết thúc game
     */
    private void finishGame() {
        game.finishGame();
        gameManager.gameDAO.finishGame(game.getId(), game.getWinnerId());
        
        // Cập nhật ranking points
        updateRankingPoints();
        
        // Cập nhật statistics
        updateUserStatistics();
        
        // Gửi kết quả game
        Message gameEndMsg = Message.notification(Message.Actions.GAME_ENDED, null);
        gameEndMsg.putData("game", game);
        gameEndMsg.putData("winnerId", game.getWinnerId());
        gameEndMsg.putData("finalScore1", game.getPlayer1Score());
        gameEndMsg.putData("finalScore2", game.getPlayer2Score());
        
        sendMessageToPlayers(gameEndMsg);
        
        logger.info("Game {} kết thúc: Winner={}, Score={}-{}", 
                   game.getId(), game.getWinnerId(), game.getPlayer1Score(), game.getPlayer2Score());
    }

    /**
     * Xử lý player guess
     */
    public void handlePlayerGuess(User user, Message message) {
        if (currentRound == null || currentRound.getRoundStatus() != GameRound.RoundStatus.GUESSING) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> colorsData = (List<Map<String, Object>>) message.getData().get("colors");
        if (colorsData == null) {
            return;
        }
        
        // Convert to GameColor list
        List<GameColor> colors = new ArrayList<>();
        for (Map<String, Object> colorData : colorsData) {
            Integer r = (Integer) colorData.get("r");
            Integer g = (Integer) colorData.get("g");
            Integer b = (Integer) colorData.get("b");
            if (r != null && g != null && b != null) {
                colors.add(new GameColor(r, g, b));
            }
        }
        
        Integer timeMillis = message.getData("timeMillis", Integer.class);
        if (timeMillis == null) timeMillis = GUESSING_DURATION;
        
        // Lưu guess
        currentGuesses.put(user.getId(), new PlayerGuess(colors, timeMillis));
        
        // Countdown latch
        if (guessingLatch != null) {
            guessingLatch.countDown();
        }
        
        logger.info("Player {} submit guess cho round {} game {}: {}", 
                   user.getUsername(), currentRound.getRoundNumber(), game.getId(), colors);
    }

    /**
     * Xử lý player leave game
     */
    public void handlePlayerLeave(User user) {
        if (!running.get()) return;
        
        running.set(false);
        game.abandonGame();
        gameManager.gameDAO.updateGameStatus(game.getId(), Game.GameStatus.ABANDONED);
        
        // Xác định winner (người còn lại)
        int winnerId = user.getId() == game.getPlayer1Id() ? game.getPlayer2Id() : game.getPlayer1Id();
        game.setWinnerId(winnerId);
        gameManager.gameDAO.finishGame(game.getId(), winnerId);
        
        // Cập nhật ranking (người thoát -0.5, người thắng +1)
        gameManager.userDAO.updateRankingPoints(user.getId(), -5); // Trừ 5 điểm
        gameManager.userDAO.updateRankingPoints(winnerId, 10);     // Cộng 10 điểm
        
        // Cập nhật statistics
        gameManager.userDAO.updateGameStats(user.getId(), false, false);     // Thua
        gameManager.userDAO.updateGameStats(winnerId, true, false);          // Thắng
        
        // Notify players
        Message abandonMsg = Message.notification(Message.Actions.GAME_ENDED, null);
        abandonMsg.putData("reason", "player_left");
        abandonMsg.putData("leftPlayer", user);
        abandonMsg.putData("winnerId", winnerId);
        
        sendMessageToPlayers(abandonMsg);
        
        logger.info("Player {} rời game {}, winner: {}", user.getUsername(), game.getId(), winnerId);
    }

    /**
     * Xử lý player disconnect
     */
    public void handlePlayerDisconnect(User user) {
        handlePlayerLeave(user); // Tương tự như leave game
    }

    /**
     * Force end game
     */
    public void forceEnd(String reason) {
        running.set(false);
        game.abandonGame();
        
        Message endMsg = Message.notification(Message.Actions.GAME_ENDED, null);
        endMsg.putData("reason", reason);
        sendMessageToPlayers(endMsg);
        
        logger.info("Force end game {}: {}", game.getId(), reason);
    }

    /**
     * Cleanup resources
     */
    private void cleanup() {
        running.set(false);
        
        // Cập nhật trạng thái user về online
        updateUserStatus(User.UserStatus.ONLINE);
        
        // Notify game manager
        gameManager.onGameFinished(this);
    }

    // Helper methods
    
    private List<GameColor> generateRandomColors() {
        List<GameColor> colors = new ArrayList<>();
        Random random = new Random();
        
        for (int i = 0; i < COLORS_COUNT; i++) {
            colors.add(AVAILABLE_COLORS.get(random.nextInt(AVAILABLE_COLORS.size())));
        }
        
        return colors;
    }

    private List<GameColor> createColorPalette() {
        // Tạo palette chứa màu target và các màu khác
        Set<GameColor> palette = new HashSet<>(AVAILABLE_COLORS);
        palette.addAll(currentRound.getTargetColors());
        
        return new ArrayList<>(palette);
    }

    private boolean checkGuess(List<GameColor> guess) {
        if (guess == null || guess.size() != COLORS_COUNT) {
            return false;
        }
        
        List<GameColor> target = new ArrayList<>(currentRound.getTargetColors());
        List<GameColor> guessColors = new ArrayList<>(guess);
        
        // Kiểm tra từng màu
        for (GameColor color : guessColors) {
            if (!target.remove(color)) {
                return false;
            }
        }
        
        return target.isEmpty();
    }

    private void ensureAllPlayersHaveGuess(long startTime) {
        if (!currentGuesses.containsKey(game.getPlayer1Id())) {
            currentGuesses.put(game.getPlayer1Id(), new PlayerGuess(Collections.emptyList(), GUESSING_DURATION));
        }
        if (!currentGuesses.containsKey(game.getPlayer2Id())) {
            currentGuesses.put(game.getPlayer2Id(), new PlayerGuess(Collections.emptyList(), GUESSING_DURATION));
        }
    }

    private void sendMessageToPlayers(Message message) {
        gameManager.server.sendMessageToUser(game.getPlayer1Id(), message);
        gameManager.server.sendMessageToUser(game.getPlayer2Id(), message);
    }

    private void updateUserStatus(User.UserStatus status) {
        gameManager.userDAO.updateUserStatus(game.getPlayer1Id(), status, true);
        gameManager.userDAO.updateUserStatus(game.getPlayer2Id(), status, true);
    }

    private void updateRankingPoints() {
        if (game.getWinnerId() == null) {
            // Hòa - cả hai +5 điểm
            gameManager.userDAO.updateRankingPoints(game.getPlayer1Id(), 5);
            gameManager.userDAO.updateRankingPoints(game.getPlayer2Id(), 5);
        } else if (game.getWinnerId() == game.getPlayer1Id()) {
            // Player 1 thắng
            gameManager.userDAO.updateRankingPoints(game.getPlayer1Id(), 10);
            gameManager.userDAO.updateRankingPoints(game.getPlayer2Id(), -5);
        } else {
            // Player 2 thắng
            gameManager.userDAO.updateRankingPoints(game.getPlayer2Id(), 10);
            gameManager.userDAO.updateRankingPoints(game.getPlayer1Id(), -5);
        }
    }

    private void updateUserStatistics() {
        if (game.getWinnerId() == null) {
            // Hòa
            gameManager.userDAO.updateGameStats(game.getPlayer1Id(), false, true);
            gameManager.userDAO.updateGameStats(game.getPlayer2Id(), false, true);
        } else if (game.getWinnerId() == game.getPlayer1Id()) {
            // Player 1 thắng
            gameManager.userDAO.updateGameStats(game.getPlayer1Id(), true, false);
            gameManager.userDAO.updateGameStats(game.getPlayer2Id(), false, false);
        } else {
            // Player 2 thắng
            gameManager.userDAO.updateGameStats(game.getPlayer2Id(), true, false);
            gameManager.userDAO.updateGameStats(game.getPlayer1Id(), false, false);
        }
    }

    // Getters
    public Game getGame() {
        return game;
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Class lưu thông tin guess của player
     */
    private static class PlayerGuess {
        final List<GameColor> colors;
        final int timeMillis;

        PlayerGuess(List<GameColor> colors, int timeMillis) {
            this.colors = colors;
            this.timeMillis = timeMillis;
        }
    }
}
