package com.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class TodoRequest {
    @NotBlank(message = "待办标题不能为空")
    private String title;

    @NotNull(message = "截止时间不能为空")
    private LocalDateTime ddl;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getDdl() { return ddl; }
    public void setDdl(LocalDateTime ddl) { this.ddl = ddl; }
}
