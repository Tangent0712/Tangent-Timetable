package com.timetable.service.impl;

import com.timetable.entity.RecurringTodo;
import com.timetable.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecurringScheduleCalculatorTest {

    private RecurringTodo rule(String frequency) {
        RecurringTodo r = new RecurringTodo();
        r.setFrequency(frequency);
        r.setTriggerTime(LocalTime.of(8, 0));
        r.setDdlOffsetMinutes(60);
        return r;
    }

    @Test
    void validateRejectsInvalidRules() {
        assertThrows(BusinessException.class, () -> {
            RecurringTodo r = rule(RecurringScheduleCalculator.WEEKLY);
            RecurringScheduleCalculator.validate(r);
        });

        assertThrows(BusinessException.class, () -> {
            RecurringTodo r = rule(RecurringScheduleCalculator.MONTHLY);
            r.setDayOfMonth(32);
            RecurringScheduleCalculator.validate(r);
        });

        assertThrows(BusinessException.class, () -> {
            RecurringTodo r = rule(RecurringScheduleCalculator.CUSTOM);
            RecurringScheduleCalculator.validate(r);
        });

        assertThrows(BusinessException.class, () -> {
            RecurringTodo r = rule(RecurringScheduleCalculator.DAILY);
            r.setDdlOffsetMinutes(0);
            RecurringScheduleCalculator.validate(r);
        });
    }

    @Test
    void dailyRollsToNextDayWhenTimePassed() {
        RecurringTodo r = rule(RecurringScheduleCalculator.DAILY);
        LocalDateTime after = LocalDateTime.of(2026, 9, 26, 12, 0);
        assertEquals(LocalDateTime.of(2026, 9, 27, 8, 0),
                RecurringScheduleCalculator.nextTriggerAfter(r, after));
    }

    @Test
    void weeklyFindsNextMatchingWeekday() {
        RecurringTodo r = rule(RecurringScheduleCalculator.WEEKLY);
        r.setDayOfWeek(1); // 周一
        LocalDateTime saturday = LocalDateTime.of(2026, 9, 26, 12, 0);
        assertEquals(LocalDateTime.of(2026, 9, 28, 8, 0),
                RecurringScheduleCalculator.nextTriggerAfter(r, saturday));
    }

    @Test
    void monthlyClampsToLastDayOfMonth() {
        RecurringTodo r = rule(RecurringScheduleCalculator.MONTHLY);
        r.setDayOfMonth(31);
        LocalDateTime feb = LocalDateTime.of(2026, 2, 1, 0, 0);
        assertEquals(LocalDateTime.of(2026, 2, 28, 8, 0),
                RecurringScheduleCalculator.nextTriggerAfter(r, feb));
    }

    @Test
    void customHasNoPrecomputedTrigger() {
        RecurringTodo r = rule(RecurringScheduleCalculator.CUSTOM);
        r.setScript("function shouldTrigger(ctx){return true;}");
        assertNull(RecurringScheduleCalculator.nextTriggerAfter(r, LocalDateTime.now()));
    }

    @Test
    void ddlIsOffsetAfterTrigger() {
        RecurringTodo r = rule(RecurringScheduleCalculator.DAILY);
        r.setDdlOffsetMinutes(1440);
        LocalDateTime fired = LocalDateTime.of(2026, 9, 26, 8, 0);
        assertEquals(LocalDateTime.of(2026, 9, 27, 8, 0),
                RecurringScheduleCalculator.ddlOf(r, fired));
    }
}
