package com.ncs.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Model class đại diện cho một trận đấu
 * Chứa thông tin về 2 người chơi và các lượt chơi
 */
public class Game {
    private int id;
    private int player1Id;
    private int player2Id;
    private User player1;
    private User player2;
    private double player1Score;
    private double player2Score;
    private Integer winnerId;
    private GameStatus gameStatus;
    private int currentRound;
    private int maxRounds;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime createdAt;
    private List<GameRound> rounds;

    public enum GameStatus {
        WAITING, PLAYING, FINISHED, ABANDONED
    }

    // Constructors
    public Game() {
        this.rounds = new ArrayList<>();
        this.player1Score = 0;
        this.player2Score = 0;
        this.currentRound = 0;
        this.maxRounds = 5;
        this.gameStatus = GameStatus.WAITING;
    }

    public Game(int player1Id, int player2Id) {
        this();
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.createdAt = LocalDateTime.now();
    }

    public Game(User player1, User player2) {
        this();
        this.player1 = player1;
        this.player2 = player2;
        this.player1Id = player1.getId();
        this.player2Id = player2.getId();
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPlayer1Id() {
        return player1Id;
    }

    public void setPlayer1Id(int player1Id) {
        this.player1Id = player1Id;
    }

    public int getPlayer2Id() {
        return player2Id;
    }

    public void setPlayer2Id(int player2Id) {
        this.player2Id = player2Id;
    }

    public User getPlayer1() {
        return player1;
    }

    public void setPlayer1(User player1) {
        this.player1 = player1;
        if (player1 != null) {
            this.player1Id = player1.getId();
        }
    }

    public User getPlayer2() {
        return player2;
    }

    public void setPlayer2(User player2) {
        this.player2 = player2;
        if (player2 != null) {
            this.player2Id = player2.getId();
        }
    }

    public double getPlayer1Score() {
        return player1Score;
    }

    public void setPlayer1Score(double player1Score) {
        this.player1Score = player1Score;
    }

    public double getPlayer2Score() {
        return player2Score;
    }

    public void setPlayer2Score(double player2Score) {
        this.player2Score = player2Score;
    }

    public Integer getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(Integer winnerId) {
        this.winnerId = winnerId;
    }

    public GameStatus getGameStatus() {
        return gameStatus;
    }

    public void setGameStatus(GameStatus gameStatus) {
        this.gameStatus = gameStatus;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public void setMaxRounds(int maxRounds) {
        this.maxRounds = maxRounds;
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

    public List<GameRound> getRounds() {
        return rounds;
    }

    public void setRounds(List<GameRound> rounds) {
        this.rounds = rounds;
    }

    // Utility methods
    public void addRound(GameRound round) {
        if (rounds == null) {
            rounds = new ArrayList<>();
        }
        rounds.add(round);
    }

    public boolean isPlayer(int userId) {
        return player1Id == userId || player2Id == userId;
    }

    public User getOpponent(int userId) {
        if (player1Id == userId) {
            return player2;
        } else if (player2Id == userId) {
            return player1;
        }
        return null;
    }

    public int getOpponentId(int userId) {
        if (player1Id == userId) {
            return player2Id;
        } else if (player2Id == userId) {
            return player1Id;
        }
        return -1;
    }

    public void addScore(int playerId, double score) {
        if (playerId == player1Id) {
            player1Score += score;
        } else if (playerId == player2Id) {
            player2Score += score;
        }
    }

    public double getPlayerScore(int playerId) {
        if (playerId == player1Id) {
            return player1Score;
        } else if (playerId == player2Id) {
            return player2Score;
        }
        return 0;
    }

    public void nextRound() {
        currentRound++;
    }

    public boolean isGameOver() {
        return currentRound >= maxRounds || gameStatus == GameStatus.FINISHED || gameStatus == GameStatus.ABANDONED;
    }

    public void determineWinner() {
        if (player1Score > player2Score) {
            winnerId = player1Id;
        } else if (player2Score > player1Score) {
            winnerId = player2Id;
        } else {
            winnerId = null; // Hòa
        }
    }

    public String getScoreDisplay() {
        return String.format("%.1f - %.1f", player1Score, player2Score);
    }

    public String getGameStatusText() {
        switch (gameStatus) {
            case WAITING:
                return "Chờ bắt đầu";
            case PLAYING:
                return "Đang chơi";
            case FINISHED:
                return "Đã kết thúc";
            case ABANDONED:
                return "Đã hủy";
            default:
                return "Không xác định";
        }
    }

    public void startGame() {
        this.gameStatus = GameStatus.PLAYING;
        this.startedAt = LocalDateTime.now();
        this.currentRound = 1;
    }

    public void finishGame() {
        this.gameStatus = GameStatus.FINISHED;
        this.finishedAt = LocalDateTime.now();
        determineWinner();
    }

    public void abandonGame() {
        this.gameStatus = GameStatus.ABANDONED;
        this.finishedAt = LocalDateTime.now();
    }

    // Override methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Game game = (Game) obj;
        return id == game.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("Game{id=%d, player1Id=%d, player2Id=%d, score=%s, status=%s, round=%d/%d}", 
                           id, player1Id, player2Id, getScoreDisplay(), gameStatus, currentRound, maxRounds);
    }
}
