package com.timetable.dto;

import jakarta.validation.constraints.NotBlank;

public class AiRenameRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
}
