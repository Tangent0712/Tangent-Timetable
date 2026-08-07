package com.timetable.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.timetable.entity.RecurringTodo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface RecurringTodoMapper extends BaseMapper<RecurringTodo> {
}
