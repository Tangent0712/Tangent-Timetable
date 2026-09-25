package com.timetable.service.impl;

import com.timetable.dto.PeriodConfigRequest;
import com.timetable.entity.ApiKey;
import com.timetable.entity.PeriodConfig;
import com.timetable.exception.BusinessException;
import com.timetable.mapper.ApiKeyMapper;
import com.timetable.mapper.PeriodConfigMapper;
import com.timetable.service.PeriodConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
public class PeriodConfigServiceImpl implements PeriodConfigService {

    private final PeriodConfigMapper periodConfigMapper;
    private final ApiKeyMapper apiKeyMapper;
    private final List<String> adminLabels;

    public PeriodConfigServiceImpl(PeriodConfigMapper periodConfigMapper,
                                   ApiKeyMapper apiKeyMapper,
                                   @Value("${app.admin-labels:Tangent0712}") String adminLabels) {
        this.periodConfigMapper = periodConfigMapper;
        this.apiKeyMapper = apiKeyMapper;
        this.adminLabels = Arrays.stream(adminLabels.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @Override
    public List<PeriodConfig> list() {
        return periodConfigMapper.selectList(null);
    }

    @Override
    @Transactional
    public List<PeriodConfig> update(PeriodConfigRequest request, String apiKey) {
        verifyAdmin(apiKey);
        for (PeriodConfigRequest.PeriodItem item : request.getConfigs()) {
            if (!item.getEndTime().isAfter(item.getStartTime())) {
                throw new BusinessException(400,
                        "第 " + item.getPeriodNumber() + " 节的结束时间必须晚于开始时间");
            }
            PeriodConfig config = new PeriodConfig();
            config.setPeriodNumber(item.getPeriodNumber());
            config.setStartTime(item.getStartTime());
            config.setEndTime(item.getEndTime());
            config.setCategory(item.getCategory());
            periodConfigMapper.updateById(config);
        }
        return periodConfigMapper.selectList(null);
    }

    /** 作息时间表为全局配置，仅管理员账号可编辑 */
    private void verifyAdmin(String apiKey) {
        ApiKey key = apiKeyMapper.selectById(apiKey);
        if (key == null || key.getLabel() == null || !adminLabels.contains(key.getLabel())) {
            throw new BusinessException(403, "仅管理员可修改作息时间表");
        }
    }
}
