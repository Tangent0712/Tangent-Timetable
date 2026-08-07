package com.timetable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timetable.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {
}
