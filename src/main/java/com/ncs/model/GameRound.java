package com.ncs.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Model class đại diện cho một lượt chơi trong trận đấu
 * Chứa thông tin về màu sắc cần đoán và kết quả của từng người chơi
 */
public class GameRound {
    private int id;
    private int gameId;
    private int roundNumber;
    private List<GameColor> targetColors;
    private List<GameColor> player1Guess;
    private List<GameColor> player2Guess;
    private Integer player1Time; // milliseconds
    private Integer player2Time; // milliseconds
    private boolean player1Correct;
    private boolean player2Correct;
    private Integer roundWinnerId;
    private RoundStatus roundStatus;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;

    public enum RoundStatus {
        WAITING, SHOWING_COLORS, GUESSING, FINISHED
    }

    // Constructors
    public GameRound() {
        this.player1Correct = false;
        this.player2Correct = false;
        this.roundStatus = RoundStatus.WAITING;
        this.createdAt = LocalDateTime.now();
    }

    public GameRound(int gameId, int roundNumber, List<GameColor> targetColors) {
        this();
        this.gameId = gameId;
        this.roundNumber = roundNumber;
        this.targetColors = targetColors;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getGameId() {
        return gameId;
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public List<GameColor> getTargetColors() {
        return targetColors;
    }

    public void setTargetColors(List<GameColor> targetColors) {
        this.targetColors = targetColors;
    }

    public List<GameColor> getPlayer1Guess() {
        return player1Guess;
    }

    public void setPlayer1Guess(List<GameColor> player1Guess) {
        this.player1Guess = player1Guess;
    }

    public List<GameColor> getPlayer2Guess() {
        return player2Guess;
    }

    public void setPlayer2Guess(List<GameColor> player2Guess) {
        this.player2Guess = player2Guess;
    }

    public Integer getPlayer1Time() {
        return player1Time;
    }

    public void setPlayer1Time(Integer player1Time) {
        this.player1Time = player1Time;
    }

    public Integer getPlayer2Time() {
        return player2Time;
    }

    public void setPlayer2Time(Integer player2Time) {
        this.player2Time = player2Time;
    }

    public boolean isPlayer1Correct() {
        return player1Correct;
    }

    public void setPlayer1Correct(boolean player1Correct) {
        this.player1Correct = player1Correct;
    }

    public boolean isPlayer2Correct() {
        return player2Correct;
    }

    public void setPlayer2Correct(boolean player2Correct) {
        this.player2Correct = player2Correct;
    }

    public Integer getRoundWinnerId() {
        return roundWinnerId;
    }

    public void setRoundWinnerId(Integer roundWinnerId) {
        this.roundWinnerId = roundWinnerId;
    }

    public RoundStatus getRoundStatus() {
        return roundStatus;
    }

    public void setRoundStatus(RoundStatus roundStatus) {
        this.roundStatus = roundStatus;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Utility methods
    public void setPlayerGuess(int playerId, List<GameColor> guess, int time) {
        if (playerId == getPlayer1Id()) {
            this.player1Guess = guess;
            this.player1Time = time;
            this.player1Correct = checkGuess(guess);
        } else if (playerId == getPlayer2Id()) {
            this.player2Guess = guess;
            this.player2Time = time;
            this.player2Correct = checkGuess(guess);
        }
    }

    private boolean checkGuess(List<GameColor> guess) {
        if (guess == null || targetColors == null || guess.size() != targetColors.size()) {
            return false;
        }
        
        // Kiểm tra xem tất cả màu trong guess có khớp với targetColors không
        // Thứ tự không quan trọng
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
        
        // Kiểm tra ngược lại để đảm bảo không có màu thừa
        for (GameColor targetColor : targetColors) {
            boolean found = false;
            for (GameColor guessColor : guess) {
                if (targetColor.equals(guessColor)) {
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

    public void determineRoundWinner(int player1Id, int player2Id) {
        if (player1Correct && !player2Correct) {
            roundWinnerId = player1Id;
        } else if (!player1Correct && player2Correct) {
            roundWinnerId = player2Id;
        } else if (player1Correct && player2Correct) {
            // Cả hai đúng, so sánh thời gian
            if (player1Time != null && player2Time != null) {
                if (player1Time < player2Time) {
                    roundWinnerId = player1Id;
                } else if (player2Time < player1Time) {
                    roundWinnerId = player2Id;
                } else {
                    roundWinnerId = null; // Hòa
                }
            } else {
                roundWinnerId = null; // Hòa nếu không có thời gian
            }
        } else {
            // Cả hai sai
            roundWinnerId = null;
        }
    }

    public double getScoreForPlayer(int playerId) {
        if (roundWinnerId == null) {
            return 0.5; // Hòa
        } else if (roundWinnerId == playerId) {
            return 1.0; // Thắng
        } else {
            return 0.0; // Thua
        }
    }

    private int getPlayer1Id() {
        // Sẽ được set từ Game object
        return -1;
    }

    private int getPlayer2Id() {
        // Sẽ được set từ Game object
        return -1;
    }

    public void startRound() {
        this.roundStatus = RoundStatus.SHOWING_COLORS;
        this.startedAt = LocalDateTime.now();
    }

    public void startGuessing() {
        this.roundStatus = RoundStatus.GUESSING;
    }

    public void finishRound() {
        this.roundStatus = RoundStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
    }

    public boolean isFinished() {
        return roundStatus == RoundStatus.FINISHED;
    }

    public String getRoundStatusText() {
        switch (roundStatus) {
            case WAITING:
                return "Chờ bắt đầu";
            case SHOWING_COLORS:
                return "Hiển thị màu";
            case GUESSING:
                return "Đoán màu";
            case FINISHED:
                return "Hoàn thành";
            default:
                return "Không xác định";
        }
    }

    // Override methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GameRound gameRound = (GameRound) obj;
        return id == gameRound.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("GameRound{id=%d, gameId=%d, roundNumber=%d, status=%s, winner=%s}", 
                           id, gameId, roundNumber, roundStatus, roundWinnerId);
    }
}

