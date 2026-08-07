package com.timetable.dto;

import com.fasterxml.jackson.databind.JsonNode;

public class AiActionDto {
    private Long actionId;
    private String type;
    private String status;
    private String scope;
    private JsonNode data;

    public AiActionDto() {}

    public AiActionDto(Long actionId, String type, String status, String scope, JsonNode data) {
        this.actionId = actionId;
        this.type = type;
        this.status = status;
        this.scope = scope;
        this.data = data;
    }

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public JsonNode getData() { return data; }
    public void setData(JsonNode data) { this.data = data; }
}
