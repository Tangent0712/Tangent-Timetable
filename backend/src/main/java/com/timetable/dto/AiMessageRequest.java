package com.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiMessageRequest {
    private Long scheduleId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 20000, message = "消息内容过长")
    private String content;

    public Long getScheduleId() { return scheduleId; }
    public void setScheduleId(Long scheduleId) { this.scheduleId = scheduleId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
