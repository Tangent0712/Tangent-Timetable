package com.timetable.service;

import com.timetable.dto.TodoRequest;
import com.timetable.entity.Todo;

import java.util.List;

public interface TodoService {
    List<Todo> list(String apiKey);
    Todo create(TodoRequest request, String apiKey);
    Todo update(Long id, TodoRequest request, String apiKey);
    void delete(Long id, String apiKey);
    Todo toggleComplete(Long id, String apiKey);

    /** 为考试自动关联一条待办：ddl=考试开始时间 */
    Todo createForExam(Long examId, String title, java.time.LocalDateTime ddl, String apiKey);
    /** 考试信息变化时同步关联待办的标题与截止时间 */
    void updateExamLinked(Long examId, String title, java.time.LocalDateTime ddl, String apiKey);
    /** 删除考试时清理其关联待办 */
    void deleteByExamId(Long examId, String apiKey);
    /** 考试结束后自动完成关联待办（幂等） */
    void completeByExamId(Long examId);
}
