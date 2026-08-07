package com.timetable.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class RecurringTodoRequest {
    @NotBlank(message = "标题不能为空")
    private String title;

    @NotBlank(message = "循环频率不能为空")
    private String frequency;

    private Integer dayOfWeek;
    private Integer dayOfMonth;

    @NotNull(message = "触发时间不能为空")
    private LocalTime triggerTime;

    @NotNull(message = "截止偏移不能为空")
    private Integer ddlOffsetMinutes;

    private Boolean enabled;
    private Boolean chainAfterComplete;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    public Integer getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }
    public LocalTime getTriggerTime() { return triggerTime; }
    public void setTriggerTime(LocalTime triggerTime) { this.triggerTime = triggerTime; }
    public Integer getDdlOffsetMinutes() { return ddlOffsetMinutes; }
    public void setDdlOffsetMinutes(Integer ddlOffsetMinutes) { this.ddlOffsetMinutes = ddlOffsetMinutes; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getChainAfterComplete() { return chainAfterComplete; }
    public void setChainAfterComplete(Boolean chainAfterComplete) { this.chainAfterComplete = chainAfterComplete; }
}
