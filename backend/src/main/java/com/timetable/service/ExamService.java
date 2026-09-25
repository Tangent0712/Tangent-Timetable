package com.timetable.service;

import com.timetable.dto.ExamRequest;
import com.timetable.entity.Exam;

import java.util.List;

public interface ExamService {
    List<Exam> listByScheduleId(Long scheduleId, String apiKey);
    Exam create(Long scheduleId, ExamRequest request, String apiKey);
    Exam update(Long id, ExamRequest request, String apiKey);
    void delete(Long id, String apiKey);
}
