package com.timetable.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

public class RecurringTodoRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 500, message = "标题过长")
    private String title;

    @NotBlank(message = "循环频率不能为空")
    private String frequency;

    @Min(value = 1, message = "星期范围为 1-7")
    @Max(value = 7, message = "星期范围为 1-7")
    private Integer dayOfWeek;

    @Min(value = 1, message = "日期范围为 1-31")
    @Max(value = 31, message = "日期范围为 1-31")
    private Integer dayOfMonth;

    /** CUSTOM 模式下可为空 */
    private LocalTime triggerTime;

    /** CUSTOM 模式下必填：用户脚本 */
    @Size(max = 4000, message = "脚本过长")
    private String script;

    @NotNull(message = "截止偏移不能为空")
    @Min(value = 1, message = "截止偏移必须为正数")
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
    public String getScript() { return script; }
    public void setScript(String script) { this.script = script; }
    public Integer getDdlOffsetMinutes() { return ddlOffsetMinutes; }
    public void setDdlOffsetMinutes(Integer ddlOffsetMinutes) { this.ddlOffsetMinutes = ddlOffsetMinutes; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getChainAfterComplete() { return chainAfterComplete; }
    public void setChainAfterComplete(Boolean chainAfterComplete) { this.chainAfterComplete = chainAfterComplete; }
}
