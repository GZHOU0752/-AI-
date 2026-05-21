package com.ruima.ai.center.model.dto;

import java.io.Serializable;
import java.util.List;

public class ChatMessage implements Serializable {
    private static final long serialVersionUID = 1L;

    private String role;
    private String content;
    private long timestamp;
    private String sessionId;
    private String userId;

    public ChatMessage() {}

    public ChatMessage(String role, String content, String sessionId) {
        this.role = role;
        this.content = content;
        this.sessionId = sessionId;
        this.timestamp = System.currentTimeMillis();
    }

    public ChatMessage(String role, String content, String sessionId, String userId) {
        this.role = role;
        this.content = content;
        this.sessionId = sessionId;
        this.userId = userId;
        this.timestamp = System.currentTimeMillis();
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRoleLabel() {
        return "user".equals(role) ? "用户" : "AI";
    }

    public static String formatHistory(List<ChatMessage> messages) {
        StringBuilder sb = new StringBuilder();
        for (ChatMessage msg : messages) {
            sb.append(msg.getRoleLabel()).append(": ").append(msg.getContent()).append("\n");
        }
        return sb.toString();
    }
}
