package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.ExamRequest;
import com.timetable.entity.Exam;
import com.timetable.interceptor.RequestContext;
import com.timetable.service.ExamService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping("/schedules/{scheduleId}/exams")
    public ApiResponse<List<Exam>> listBySchedule(@PathVariable Long scheduleId) {
        return ApiResponse.success(examService.listByScheduleId(scheduleId, RequestContext.getApiKey()));
    }

    @PostMapping("/schedules/{scheduleId}/exams")
    public ApiResponse<Exam> create(@PathVariable Long scheduleId, @Valid @RequestBody ExamRequest request) {
        return ApiResponse.success(examService.create(scheduleId, request, RequestContext.getApiKey()));
    }

    @PutMapping("/exams/{id}")
    public ApiResponse<Exam> update(@PathVariable Long id, @Valid @RequestBody ExamRequest request) {
        return ApiResponse.success(examService.update(id, request, RequestContext.getApiKey()));
    }

    @DeleteMapping("/exams/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        examService.delete(id, RequestContext.getApiKey());
        return ApiResponse.success(null);
    }
}
