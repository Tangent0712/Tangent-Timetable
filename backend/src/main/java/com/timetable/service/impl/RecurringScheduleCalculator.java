package com.timetable.service.impl;

import com.timetable.entity.RecurringTodo;
import com.timetable.exception.BusinessException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 循环待办的触发时间计算。
 */
public final class RecurringScheduleCalculator {

    public static final String DAILY = "DAILY";
    public static final String WEEKLY = "WEEKLY";
    public static final String MONTHLY = "MONTHLY";
    /** 自定义脚本模式：触发时机由用户脚本 shouldTrigger(ctx) 决定 */
    public static final String CUSTOM = "CUSTOM";

    private RecurringScheduleCalculator() {}

    public static void validate(RecurringTodo rule) {
        String freq = rule.getFrequency();
        if (freq == null) {
            throw new BusinessException(400, "循环频率不能为空");
        }
        switch (freq) {
            case DAILY -> { /* 无需额外字段 */ }
            case WEEKLY -> {
                if (rule.getDayOfWeek() == null || rule.getDayOfWeek() < 1 || rule.getDayOfWeek() > 7) {
                    throw new BusinessException(400, "每周循环需要指定星期（1=周一 ... 7=周日）");
                }
            }
            case MONTHLY -> {
                if (rule.getDayOfMonth() == null || rule.getDayOfMonth() < 1 || rule.getDayOfMonth() > 31) {
                    throw new BusinessException(400, "每月循环需要指定日期（1-31）");
                }
            }
            case CUSTOM -> {
                if (rule.getScript() == null || rule.getScript().isBlank()) {
                    throw new BusinessException(400, "自定义规则必须提供脚本");
                }
            }
            default -> throw new BusinessException(400, "不支持的循环频率: " + freq);
        }
        // CUSTOM 由脚本决定触发时机，不需要固定时刻
        if (!CUSTOM.equals(freq) && rule.getTriggerTime() == null) {
            throw new BusinessException(400, "触发时间不能为空");
        }
        if (rule.getDdlOffsetMinutes() == null || rule.getDdlOffsetMinutes() <= 0) {
            throw new BusinessException(400, "截止时间必须晚于触发时间");
        }
    }

    /**
     * 计算严格晚于 after 的下一次触发时间。
     * CUSTOM 模式返回 null —— 它不做提前推算，由调度器每分钟执行脚本判断。
     */
    public static LocalDateTime nextTriggerAfter(RecurringTodo rule, LocalDateTime after) {
        if (CUSTOM.equals(rule.getFrequency())) {
            return null;
        }
        LocalDateTime candidate = switch (rule.getFrequency()) {
            case DAILY -> after.toLocalDate().atTime(rule.getTriggerTime());
            case WEEKLY -> {
                LocalDate date = after.toLocalDate();
                int diff = rule.getDayOfWeek() - date.getDayOfWeek().getValue();
                if (diff < 0) {
                    diff += 7;
                }
                yield date.plusDays(diff).atTime(rule.getTriggerTime());
            }
            case MONTHLY -> atDayOfMonth(after.toLocalDate(), rule.getDayOfMonth())
                    .atTime(rule.getTriggerTime());
            default -> throw new BusinessException(400, "不支持的循环频率: " + rule.getFrequency());
        };

        if (!candidate.isAfter(after)) {
            candidate = switch (rule.getFrequency()) {
                case DAILY -> candidate.plusDays(1);
                case WEEKLY -> candidate.plusWeeks(1);
                case MONTHLY -> atDayOfMonth(
                        candidate.toLocalDate().withDayOfMonth(1).plusMonths(1),
                        rule.getDayOfMonth()).atTime(rule.getTriggerTime());
                default -> candidate;
            };
        }
        return candidate;
    }

    /** 日期超出当月天数时取当月最后一天（如 31 号规则在 2 月落到 28/29 号） */
    private static LocalDate atDayOfMonth(LocalDate base, int dayOfMonth) {
        int max = base.lengthOfMonth();
        return base.withDayOfMonth(Math.min(dayOfMonth, max));
    }

    public static LocalDateTime ddlOf(RecurringTodo rule, LocalDateTime triggeredAt) {
        return triggeredAt.plus(rule.getDdlOffsetMinutes(), ChronoUnit.MINUTES);
    }

}
