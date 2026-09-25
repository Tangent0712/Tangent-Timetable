package com.timetable.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.timetable.entity.Exam;
import com.timetable.mapper.ExamMapper;
import com.timetable.service.TodoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考试关联待办的自动完成器。
 *
 * 每场考试都会自动生成一条待办（ddl=考试开始时间），
 * 本调度器每分钟扫描一次，考试结束（examDate + endTime 已过）后
 * 把关联且未完成的待办标记为完成。启动时补扫一次。
 */
@Component
public class ExamTodoScheduler {

    private static final Logger log = LoggerFactory.getLogger(ExamTodoScheduler.class);

    private final ExamMapper examMapper;
    private final TodoService todoService;

    public ExamTodoScheduler(ExamMapper examMapper, TodoService todoService) {
        this.examMapper = examMapper;
        this.todoService = todoService;
    }

    @Scheduled(fixedDelay = 60_000L)
    public void tick() {
        try {
            completeFinishedExams();
        } catch (Exception e) {
            log.error("Failed to complete finished exam todos", e);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        try {
            int n = completeFinishedExams();
            if (n > 0) {
                log.info("Startup catch-up completed {} exam-linked todo(s)", n);
            }
        } catch (Exception e) {
            log.error("Startup catch-up failed", e);
        }
    }

    private int completeFinishedExams() {
        LocalDateTime now = LocalDateTime.now();
        // 只取日期不晚于今天的考试，避免全表扫描
        LambdaQueryWrapper<Exam> wrapper = new LambdaQueryWrapper<>();
        wrapper.le(Exam::getExamDate, now.toLocalDate());
        List<Exam> exams = examMapper.selectList(wrapper);
        int completed = 0;
        for (Exam e : exams) {
            if (e.getExamDate() == null || e.getEndTime() == null) {
                continue;
            }
            LocalDateTime end = LocalDateTime.of(e.getExamDate(), e.getEndTime());
            if (end.isAfter(now)) {
                continue;
            }
            todoService.completeByExamId(e.getId());
            completed++;
        }
        return completed;
    }
}
