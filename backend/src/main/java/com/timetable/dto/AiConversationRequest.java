package com.timetable.dto;

public class AiConversationRequest {
    private Long scheduleId;
    private String title;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
