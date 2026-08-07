package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.ScheduleRequest;
import com.timetable.entity.Course;
import com.timetable.entity.Schedule;
import com.timetable.interceptor.RequestContext;
import com.timetable.service.CourseService;
import com.timetable.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final CourseService courseService;

    public ScheduleController(ScheduleService scheduleService, CourseService courseService) {
        this.scheduleService = scheduleService;
        this.courseService = courseService;
    }

    @GetMapping
    public ApiResponse<List<Schedule>> list() {
        return ApiResponse.success(scheduleService.list(RequestContext.getApiKey()));
    }

    @PostMapping
    public ApiResponse<Schedule> create(@Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.success(scheduleService.create(request, RequestContext.getApiKey()));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getById(@PathVariable Long id) {
        String apiKey = RequestContext.getApiKey();
        Schedule schedule = scheduleService.getById(id, apiKey);
        List<Course> courses = courseService.listByScheduleId(id, apiKey);
        Map<String, Object> result = new HashMap<>();
        result.put("schedule", schedule);
        result.put("courses", courses);
        return ApiResponse.success(result);
    }

    @PutMapping("/{id}")
    public ApiResponse<Schedule> update(@PathVariable Long id, @Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.success(scheduleService.update(id, request, RequestContext.getApiKey()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id, RequestContext.getApiKey());
        return ApiResponse.success(null);
    }

    @GetMapping("/{id}/courses")
    public ApiResponse<List<Course>> listCourses(@PathVariable Long id) {
        return ApiResponse.success(courseService.listByScheduleId(id, RequestContext.getApiKey()));
    }
}
