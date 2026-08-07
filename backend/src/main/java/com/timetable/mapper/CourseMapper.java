package com.timetable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timetable.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
