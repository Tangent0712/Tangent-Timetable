package com.timetable.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 循环待办规则。到达触发时间时自动生成一条真实的 todo 记录。
 *
 * 例：每周五 12:00 触发「刷本周网课」，截止本周日 12:00
 * → frequency=WEEKLY, dayOfWeek=5, triggerTime=12:00, ddlOffsetMinutes=2880（2天）
 */
@TableName("recurring_todo")
public class RecurringTodo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String apiKey;
    private String title;
    /** DAILY / WEEKLY / MONTHLY / CUSTOM */
    private String frequency;
    /** WEEKLY 时有效，1=周一 ... 7=周日 */
    private Integer dayOfWeek;
    /** MONTHLY 时有效，1-31；超出当月天数时取当月最后一天 */
    private Integer dayOfMonth;
    /** 每次触发的时刻；CUSTOM 模式下为 null，由脚本自行判断 */
    private LocalTime triggerTime;
    /** CUSTOM 模式下的用户脚本，需定义 shouldTrigger(ctx) 返回布尔值 */
    private String script;
    /** 截止时间 = 触发时间 + 该分钟数 */
    private Integer ddlOffsetMinutes;
    private Boolean enabled;
    /** 为 true 时，用户完成当期待办后立即生成下一期；为 false 时只按触发时间生成 */
    private Boolean chainAfterComplete;
    private LocalDateTime lastTriggeredAt;
    private LocalDateTime nextTriggerAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
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
    public LocalDateTime getLastTriggeredAt() { return lastTriggeredAt; }
    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }
    public LocalDateTime getNextTriggerAt() { return nextTriggerAt; }
    public void setNextTriggerAt(LocalDateTime nextTriggerAt) { this.nextTriggerAt = nextTriggerAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
