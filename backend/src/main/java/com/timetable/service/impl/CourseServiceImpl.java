package com.timetable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.timetable.dto.BatchCourseRequest;
import com.timetable.dto.CourseRequest;
import com.timetable.entity.Course;
import com.timetable.entity.Schedule;
import com.timetable.exception.BusinessException;
import com.timetable.mapper.CourseMapper;
import com.timetable.mapper.ScheduleMapper;
import com.timetable.service.CourseService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final ScheduleMapper scheduleMapper;

    public CourseServiceImpl(CourseMapper courseMapper, ScheduleMapper scheduleMapper) {
        this.courseMapper = courseMapper;
        this.scheduleMapper = scheduleMapper;
    }

    private void verifyScheduleOwnership(Long scheduleId, String apiKey) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null || !schedule.getApiKey().equals(apiKey)) {
            throw new BusinessException(404, "课表不存在");
        }
    }

    @Override
    public List<Course> listByScheduleId(Long scheduleId, String apiKey) {
        verifyScheduleOwnership(scheduleId, apiKey);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Course::getScheduleId, scheduleId).orderByAsc(Course::getDayOfWeek, Course::getStartPeriod);
        return courseMapper.selectList(wrapper);
    }

    @Override
    public Course create(Long scheduleId, CourseRequest request, String apiKey) {
        verifyScheduleOwnership(scheduleId, apiKey);
        Course course = new Course();
        course.setScheduleId(scheduleId);
        course.setName(request.getName());
        course.setLocation(request.getLocation());
        course.setTeacher(request.getTeacher());
        course.setDayOfWeek(request.getDayOfWeek());
        course.setStartPeriod(request.getStartPeriod());
        course.setEndPeriod(request.getEndPeriod());
        course.setWeeks(request.getWeeks());
        courseMapper.insert(course);
        return course;
    }

    @Override
    @Transactional
    public List<Course> batchCreate(Long scheduleId, BatchCourseRequest request, String apiKey) {
        verifyScheduleOwnership(scheduleId, apiKey);
        List<Course> result = new ArrayList<>();
        for (CourseRequest req : request.getCourses()) {
            Course course = new Course();
            course.setScheduleId(scheduleId);
            course.setName(req.getName());
            course.setLocation(req.getLocation());
            course.setTeacher(req.getTeacher());
            course.setDayOfWeek(req.getDayOfWeek());
            course.setStartPeriod(req.getStartPeriod());
            course.setEndPeriod(req.getEndPeriod());
            course.setWeeks(req.getWeeks());
            courseMapper.insert(course);
            result.add(course);
        }
        return result;
    }

    @Override
    public Course update(Long id, CourseRequest request, String apiKey) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        verifyScheduleOwnership(course.getScheduleId(), apiKey);
        course.setName(request.getName());
        course.setLocation(request.getLocation());
        course.setTeacher(request.getTeacher());
        course.setDayOfWeek(request.getDayOfWeek());
        course.setStartPeriod(request.getStartPeriod());
        course.setEndPeriod(request.getEndPeriod());
        course.setWeeks(request.getWeeks());
        courseMapper.updateById(course);
        return course;
    }

    @Override
    public void delete(Long id, String apiKey) {
        Course course = courseMapper.selectById(id);
        if (course == null) {
            throw new BusinessException(404, "课程不存在");
        }
        verifyScheduleOwnership(course.getScheduleId(), apiKey);
        courseMapper.deleteById(id);
    }
}
