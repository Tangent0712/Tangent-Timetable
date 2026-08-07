package com.timetable.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AiMessageResponse {
    private Long messageId;
    private String role;
    private String text;
    private List<AiActionDto> actions = new ArrayList<>();
    private LocalDateTime createdAt;

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public List<AiActionDto> getActions() { return actions; }
    public void setActions(List<AiActionDto> actions) { this.actions = actions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
