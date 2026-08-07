package com.timetable.config;

import com.timetable.service.RecurringTodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 循环待办的定时生成器。
 *
 * 每分钟扫描一次到期规则；启动时额外扫描一次，补齐停机期间错过的触发。
 */
@Component
public class RecurringTodoScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringTodoScheduler.class);

    private final RecurringTodoService recurringTodoService;

    public RecurringTodoScheduler(RecurringTodoService recurringTodoService) {
        this.recurringTodoService = recurringTodoService;
    }

    @Scheduled(fixedDelay = 60_000L)
    public void tick() {
        try {
            recurringTodoService.generateDueTodos();
        } catch (Exception e) {
            log.error("Failed to generate recurring todos", e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        try {
            int n = recurringTodoService.generateDueTodos();
            if (n > 0) {
                log.info("Startup catch-up generated {} recurring todo(s)", n);
            }
        } catch (Exception e) {
            log.error("Startup catch-up failed", e);
        }
    }
}
