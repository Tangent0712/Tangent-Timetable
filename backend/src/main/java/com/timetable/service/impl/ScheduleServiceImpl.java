package com.timetable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.timetable.dto.ScheduleRequest;
import com.timetable.entity.Schedule;
import com.timetable.exception.BusinessException;
import com.timetable.mapper.ScheduleMapper;
import com.timetable.service.ScheduleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final ScheduleMapper scheduleMapper;

    public ScheduleServiceImpl(ScheduleMapper scheduleMapper) {
        this.scheduleMapper = scheduleMapper;
    }

    @Override
    public List<Schedule> list(String apiKey) {
        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Schedule::getApiKey, apiKey).orderByDesc(Schedule::getCreatedAt);
        return scheduleMapper.selectList(wrapper);
    }

    @Override
    public Schedule getById(Long id, String apiKey) {
        Schedule schedule = scheduleMapper.selectById(id);
        if (schedule == null || !schedule.getApiKey().equals(apiKey)) {
            throw new BusinessException(404, "课表不存在");
        }
        return schedule;
    }

    private void validateDateRange(ScheduleRequest request) {
        if (request.getPeriodStartDate() != null && request.getPeriodEndDate() != null
                && request.getPeriodEndDate().isBefore(request.getPeriodStartDate())) {
            throw new BusinessException(400, "学期结束日期不能早于开始日期");
        }
    }

    @Override
    public Schedule create(ScheduleRequest request, String apiKey) {
        validateDateRange(request);
        Schedule schedule = new Schedule();
        schedule.setApiKey(apiKey);
        schedule.setName(request.getName());
        schedule.setPeriodStartDate(request.getPeriodStartDate());
        schedule.setPeriodEndDate(request.getPeriodEndDate());
        scheduleMapper.insert(schedule);
        return schedule;
    }

    @Override
    public Schedule update(Long id, ScheduleRequest request, String apiKey) {
        validateDateRange(request);
        Schedule schedule = getById(id, apiKey);
        schedule.setName(request.getName());
        schedule.setPeriodStartDate(request.getPeriodStartDate());
        schedule.setPeriodEndDate(request.getPeriodEndDate());
        scheduleMapper.updateById(schedule);
        return schedule;
    }

    @Override
    public void delete(Long id, String apiKey) {
        getById(id, apiKey);
        scheduleMapper.deleteById(id);
    }
}
