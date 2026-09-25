package com.timetable.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("todo")
public class Todo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String apiKey;
    private String title;
    private LocalDateTime ddl;
    private Boolean completed;
    private Long recurringId;
    /** 非空表示由某条考试自动关联生成的待办（ddl=考试开始，考试结束后自动完成） */
    private Long examId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public LocalDateTime getDdl() { return ddl; }
    public void setDdl(LocalDateTime ddl) { this.ddl = ddl; }
    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public Long getRecurringId() { return recurringId; }
    public void setRecurringId(Long recurringId) { this.recurringId = recurringId; }
    public Long getExamId() { return examId; }
    public void setExamId(Long examId) { this.examId = examId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
