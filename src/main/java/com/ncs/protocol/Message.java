package com.ncs.protocol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.Map;

/**
 * Class đại diện cho message trao đổi giữa client và server
 * Sử dụng JSON format để serialize/deserialize
 */
public class Message {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private MessageType type;
    private String action;
    private Map<String, Object> data;
    private boolean success;
    private String message;
    private long timestamp;

    public enum MessageType {
        REQUEST, RESPONSE, NOTIFICATION, ERROR
    }

    // Constructors
    public Message() {
        this.timestamp = System.currentTimeMillis();
    }

    @JsonCreator
    public Message(@JsonProperty("type") MessageType type,
                   @JsonProperty("action") String action,
                   @JsonProperty("data") Map<String, Object> data) {
        this();
        this.type = type;
        this.action = action;
        this.data = data;
    }

    public Message(MessageType type, String action) {
        this(type, action, null);
    }

    // Factory methods cho các loại message phổ biến
    public static Message request(String action, Map<String, Object> data) {
        return new Message(MessageType.REQUEST, action, data);
    }

    public static Message response(String action, Map<String, Object> data, boolean success) {
        Message msg = new Message(MessageType.RESPONSE, action, data);
        msg.setSuccess(success);
        return msg;
    }

    public static Message notification(String action, Map<String, Object> data) {
        return new Message(MessageType.NOTIFICATION, action, data);
    }

    public static Message error(String action, String errorMessage) {
        Message msg = new Message(MessageType.ERROR, action, null);
        msg.setSuccess(false);
        msg.setMessage(errorMessage);
        return msg;
    }

    // Getters and Setters
    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Utility methods
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, Class<T> type) {
        if (data == null || !data.containsKey(key)) {
            return null;
        }
        
        Object value = data.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        
        return null;
    }

    public void putData(String key, Object value) {
        if (data == null) {
            data = new java.util.HashMap<>();
        }
        data.put(key, value);
    }

    public boolean hasData(String key) {
        return data != null && data.containsKey(key);
    }

    // JSON serialization
    public String toJson() {
        try {
            return MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing message to JSON", e);
        }
    }

    public static Message fromJson(String json) {
        try {
            return MAPPER.readValue(json, Message.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing message from JSON", e);
        }
    }

    // Override methods
    @Override
    public String toString() {
        return String.format("Message{type=%s, action='%s', success=%s, message='%s', timestamp=%d}", 
                           type, action, success, message, timestamp);
    }

    // Common actions constants
    public static class Actions {
        // Authentication
        public static final String LOGIN = "login";
        public static final String REGISTER = "register";
        public static final String LOGOUT = "logout";
        
        // User management
        public static final String GET_ONLINE_USERS = "get_online_users";
        public static final String GET_LEADERBOARD = "get_leaderboard";
        public static final String UPDATE_USER_STATUS = "update_user_status";
        
        // Game invitations
        public static final String SEND_INVITATION = "send_invitation";
        public static final String ACCEPT_INVITATION = "accept_invitation";
        public static final String REJECT_INVITATION = "reject_invitation";
        public static final String INVITATION_RECEIVED = "invitation_received";
        public static final String INVITATION_RESPONSE = "invitation_response";
        
        // Game management
        public static final String START_GAME = "start_game";
        public static final String JOIN_GAME = "join_game";
        public static final String LEAVE_GAME = "leave_game";
        public static final String GAME_STARTED = "game_started";
        public static final String GAME_ENDED = "game_ended";
        
        // Game rounds
        public static final String START_ROUND = "start_round";
        public static final String SHOW_COLORS = "show_colors";
        public static final String START_GUESSING = "start_guessing";
        public static final String SUBMIT_GUESS = "submit_guess";
        public static final String ROUND_RESULT = "round_result";
        public static final String ROUND_FINISHED = "round_finished";
        
        // Rematch
        public static final String REQUEST_REMATCH = "request_rematch";
        public static final String ACCEPT_REMATCH = "accept_rematch";
        public static final String REJECT_REMATCH = "reject_rematch";
        public static final String REMATCH_RESPONSE = "rematch_response";
        
        // System notifications
        public static final String USER_JOINED = "user_joined";
        public static final String USER_LEFT = "user_left";
        public static final String USER_STATUS_CHANGED = "user_status_changed";
        public static final String ERROR = "error";
        public static final String HEARTBEAT = "heartbeat";
    }
}
