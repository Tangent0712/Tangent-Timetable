package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.BatchCourseRequest;
import com.timetable.dto.CourseRequest;
import com.timetable.entity.Course;
import com.timetable.interceptor.RequestContext;
import com.timetable.service.AiService;
import com.timetable.service.impl.AiServiceImpl;
import com.timetable.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService courseService;
    private final AiService aiService;

    public CourseController(CourseService courseService, AiService aiService) {
        this.courseService = courseService;
        this.aiService = aiService;
    }

    @PostMapping("/schedules/{scheduleId}/courses")
    public ApiResponse<Course> create(@PathVariable Long scheduleId, @Valid @RequestBody CourseRequest request) {
        String apiKey = RequestContext.getApiKey();
        Course course = courseService.create(scheduleId, request, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_COURSE);
        return ApiResponse.success(course);
    }

    @PostMapping("/schedules/{scheduleId}/courses/batch")
    public ApiResponse<List<Course>> batchCreate(@PathVariable Long scheduleId, @Valid @RequestBody BatchCourseRequest request) {
        String apiKey = RequestContext.getApiKey();
        List<Course> courses = courseService.batchCreate(scheduleId, request, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_COURSE);
        return ApiResponse.success(courses);
    }

    @PutMapping("/courses/{id}")
    public ApiResponse<Course> update(@PathVariable Long id, @Valid @RequestBody CourseRequest request) {
        String apiKey = RequestContext.getApiKey();
        Course course = courseService.update(id, request, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_COURSE);
        return ApiResponse.success(course);
    }

    @DeleteMapping("/courses/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String apiKey = RequestContext.getApiKey();
        courseService.delete(id, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_COURSE);
        return ApiResponse.success(null);
    }
}
