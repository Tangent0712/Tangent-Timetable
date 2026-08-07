package com.timetable.service.impl;

import com.timetable.dto.PeriodConfigRequest;
import com.timetable.entity.PeriodConfig;
import com.timetable.mapper.PeriodConfigMapper;
import com.timetable.service.PeriodConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PeriodConfigServiceImpl implements PeriodConfigService {

    private final PeriodConfigMapper periodConfigMapper;

    public PeriodConfigServiceImpl(PeriodConfigMapper periodConfigMapper) {
        this.periodConfigMapper = periodConfigMapper;
    }

    @Override
    public List<PeriodConfig> list() {
        return periodConfigMapper.selectList(null);
    }

    @Override
    @Transactional
    public List<PeriodConfig> update(PeriodConfigRequest request) {
        List<PeriodConfig> result = new ArrayList<>();
        for (PeriodConfigRequest.PeriodItem item : request.getConfigs()) {
            PeriodConfig config = new PeriodConfig();
            config.setPeriodNumber(item.getPeriodNumber());
            config.setStartTime(item.getStartTime());
            config.setEndTime(item.getEndTime());
            config.setCategory(item.getCategory());
            periodConfigMapper.updateById(config);
            result.add(periodConfigMapper.selectById(item.getPeriodNumber()));
        }
        return result;
    }
}
