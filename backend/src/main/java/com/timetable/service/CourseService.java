package com.timetable.service;

import com.timetable.dto.BatchCourseRequest;
import com.timetable.dto.CourseRequest;
import com.timetable.entity.Course;

import java.util.List;

public interface CourseService {
    List<Course> listByScheduleId(Long scheduleId, String apiKey);
    Course create(Long scheduleId, CourseRequest request, String apiKey);
    List<Course> batchCreate(Long scheduleId, BatchCourseRequest request, String apiKey);
    Course update(Long id, CourseRequest request, String apiKey);
    void delete(Long id, String apiKey);
}
