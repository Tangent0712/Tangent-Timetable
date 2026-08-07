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
            default -> throw new BusinessException(400, "不支持的循环频率: " + freq);
        }
        if (rule.getTriggerTime() == null) {
            throw new BusinessException(400, "触发时间不能为空");
        }
        if (rule.getDdlOffsetMinutes() == null || rule.getDdlOffsetMinutes() <= 0) {
            throw new BusinessException(400, "截止时间必须晚于触发时间");
        }
    }

    /**
     * 计算严格晚于 after 的下一次触发时间。
     */
    public static LocalDateTime nextTriggerAfter(RecurringTodo rule, LocalDateTime after) {
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

    /** 人类可读的频率描述，用于前端与 AI Prompt */
    public static String describe(RecurringTodo rule) {
        String time = rule.getTriggerTime() == null ? "?" : rule.getTriggerTime().toString();
        String when = switch (rule.getFrequency() == null ? "" : rule.getFrequency()) {
            case DAILY -> "每天 " + time;
            case WEEKLY -> "每周" + AiPromptBuilder.weekDayName(
                    rule.getDayOfWeek() == null ? 0 : rule.getDayOfWeek()).replace("周", "") + " " + time;
            case MONTHLY -> "每月 " + rule.getDayOfMonth() + " 日 " + time;
            default -> "未知频率";
        };
        return when + " 触发，" + describeOffset(rule.getDdlOffsetMinutes()) + "后截止";
    }

    public static String describeOffset(Integer minutes) {
        if (minutes == null) {
            return "?";
        }
        int days = minutes / 1440;
        int hours = (minutes % 1440) / 60;
        int mins = minutes % 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" 天");
        if (hours > 0) sb.append(hours).append(" 小时");
        if (mins > 0) sb.append(mins).append(" 分钟");
        return sb.length() == 0 ? "0 分钟" : sb.toString();
    }
}
