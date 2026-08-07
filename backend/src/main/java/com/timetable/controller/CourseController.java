package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.BatchCourseRequest;
import com.timetable.dto.CourseRequest;
import com.timetable.entity.Course;
import com.timetable.interceptor.RequestContext;
import com.timetable.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping("/schedules/{scheduleId}/courses")
    public ApiResponse<Course> create(@PathVariable Long scheduleId, @Valid @RequestBody CourseRequest request) {
        return ApiResponse.success(courseService.create(scheduleId, request, RequestContext.getApiKey()));
    }

    @PostMapping("/schedules/{scheduleId}/courses/batch")
    public ApiResponse<List<Course>> batchCreate(@PathVariable Long scheduleId, @Valid @RequestBody BatchCourseRequest request) {
        return ApiResponse.success(courseService.batchCreate(scheduleId, request, RequestContext.getApiKey()));
    }

    @PutMapping("/courses/{id}")
    public ApiResponse<Course> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        return ApiResponse.success(courseService.update(id, request, RequestContext.getApiKey()));
    }

    @DeleteMapping("/courses/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        courseService.delete(id, RequestContext.getApiKey());
        return ApiResponse.success(null);
    }
}
