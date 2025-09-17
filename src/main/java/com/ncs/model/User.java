package com.ncs.model;

import java.time.LocalDateTime;

/**
 * Model class đại diện cho người dùng trong hệ thống
 * Theo mô hình MVC, class này chỉ chứa dữ liệu và các getter/setter
 */
public class User {
    private int id;
    private String username;
    private String password;
    private String email;
    private String fullName;
    private int rankingPoints;
    private int totalGames;
    private int wins;
    private int losses;
    private int draws;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isOnline;
    private UserStatus status;

    public enum UserStatus {
        ONLINE, OFFLINE, PLAYING
    }

    // Constructors
    public User() {}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.rankingPoints = 0;
        this.totalGames = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.isOnline = false;
        this.status = UserStatus.OFFLINE;
    }

    public User(int id, String username, String fullName, int rankingPoints, 
                int totalGames, int wins, int losses, int draws, UserStatus status) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.rankingPoints = rankingPoints;
        this.totalGames = totalGames;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.status = status;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getRankingPoints() {
        return rankingPoints;
    }

    public void setRankingPoints(int rankingPoints) {
        this.rankingPoints = Math.max(0, rankingPoints); // Điểm không được âm
    }

    public int getTotalGames() {
        return totalGames;
    }

    public void setTotalGames(int totalGames) {
        this.totalGames = totalGames;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public int getDraws() {
        return draws;
    }

    public void setDraws(int draws) {
        this.draws = draws;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    // Utility methods
    public double getWinRate() {
        if (totalGames == 0) return 0.0;
        return (double) wins / totalGames * 100;
    }

    public String getDisplayName() {
        return fullName != null && !fullName.trim().isEmpty() ? fullName : username;
    }

    public String getStatusText() {
        switch (status) {
            case ONLINE:
                return "Trực tuyến";
            case PLAYING:
                return "Đang chơi";
            case OFFLINE:
            default:
                return "Ngoại tuyến";
        }
    }

    // Override methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return id == user.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("User{id=%d, username='%s', fullName='%s', rankingPoints=%d, status=%s}", 
                           id, username, fullName, rankingPoints, status);
    }
}

