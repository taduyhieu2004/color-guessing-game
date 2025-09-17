package com.ncs.model;

import java.time.LocalDateTime;

/**
 * Model class đại diện cho lời mời thách đấu
 * Chứa thông tin về người mời, người được mời và trạng thái lời mời
 */
public class GameInvitation {
    private int id;
    private int inviterId;
    private int inviteeId;
    private User inviter;
    private User invitee;
    private Integer gameId;
    private InvitationStatus status;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime respondedAt;
    private LocalDateTime expiresAt;

    public enum InvitationStatus {
        PENDING, ACCEPTED, REJECTED, EXPIRED
    }

    // Constructors
    public GameInvitation() {
        this.status = InvitationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = LocalDateTime.now().plusMinutes(2); // Hết hạn sau 2 phút
    }

    public GameInvitation(int inviterId, int inviteeId) {
        this();
        this.inviterId = inviterId;
        this.inviteeId = inviteeId;
    }

    public GameInvitation(User inviter, User invitee) {
        this();
        this.inviter = inviter;
        this.invitee = invitee;
        this.inviterId = inviter.getId();
        this.inviteeId = invitee.getId();
    }

    public GameInvitation(User inviter, User invitee, String message) {
        this(inviter, invitee);
        this.message = message;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInviterId() {
        return inviterId;
    }

    public void setInviterId(int inviterId) {
        this.inviterId = inviterId;
    }

    public int getInviteeId() {
        return inviteeId;
    }

    public void setInviteeId(int inviteeId) {
        this.inviteeId = inviteeId;
    }

    public User getInviter() {
        return inviter;
    }

    public void setInviter(User inviter) {
        this.inviter = inviter;
        if (inviter != null) {
            this.inviterId = inviter.getId();
        }
    }

    public User getInvitee() {
        return invitee;
    }

    public void setInvitee(User invitee) {
        this.invitee = invitee;
        if (invitee != null) {
            this.inviteeId = invitee.getId();
        }
    }

    public Integer getGameId() {
        return gameId;
    }

    public void setGameId(Integer gameId) {
        this.gameId = gameId;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
        if (status != InvitationStatus.PENDING) {
            this.respondedAt = LocalDateTime.now();
        }
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    // Utility methods
    public void accept() {
        setStatus(InvitationStatus.ACCEPTED);
    }

    public void reject() {
        setStatus(InvitationStatus.REJECTED);
    }

    public void expire() {
        setStatus(InvitationStatus.EXPIRED);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt) || status == InvitationStatus.EXPIRED;
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING && !isExpired();
    }

    public boolean isAccepted() {
        return status == InvitationStatus.ACCEPTED;
    }

    public boolean isRejected() {
        return status == InvitationStatus.REJECTED;
    }

    public String getStatusText() {
        switch (status) {
            case PENDING:
                return isExpired() ? "Hết hạn" : "Chờ phản hồi";
            case ACCEPTED:
                return "Đã chấp nhận";
            case REJECTED:
                return "Đã từ chối";
            case EXPIRED:
                return "Hết hạn";
            default:
                return "Không xác định";
        }
    }

    public String getInvitationMessage() {
        if (message != null && !message.trim().isEmpty()) {
            return message;
        }
        
        String inviterName = inviter != null ? inviter.getDisplayName() : "Người chơi";
        return inviterName + " mời bạn thách đấu!";
    }

    public long getTimeUntilExpiry() {
        if (isExpired()) {
            return 0;
        }
        return java.time.Duration.between(LocalDateTime.now(), expiresAt).getSeconds();
    }

    // Override methods
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GameInvitation that = (GameInvitation) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("GameInvitation{id=%d, inviter=%d, invitee=%d, status=%s, gameId=%s}", 
                           id, inviterId, inviteeId, status, gameId);
    }
}

