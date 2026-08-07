package com.timetable.service;

import com.timetable.dto.ScheduleRequest;
import com.timetable.entity.Schedule;

import java.util.List;

public interface ScheduleService {
    List<Schedule> list(String apiKey);
    Schedule getById(Long id, String apiKey);
    Schedule create(ScheduleRequest request, String apiKey);
    Schedule update(Long id, ScheduleRequest request, String apiKey);
    void delete(Long id, String apiKey);
}
