package com.timetable.service;

import com.timetable.dto.PeriodConfigRequest;
import com.timetable.entity.PeriodConfig;

import java.util.List;

public interface PeriodConfigService {
    List<PeriodConfig> list();
    List<PeriodConfig> update(PeriodConfigRequest request, String apiKey);
}
