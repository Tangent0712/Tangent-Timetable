package com.timetable.service.impl;

import com.timetable.dto.AuthVerifyResponse;
import com.timetable.entity.ApiKey;
import com.timetable.mapper.ApiKeyMapper;
import com.timetable.service.ApiKeyService;
import org.springframework.stereotype.Service;

@Service
public class ApiKeyServiceImpl implements ApiKeyService {

    private final ApiKeyMapper apiKeyMapper;

    public ApiKeyServiceImpl(ApiKeyMapper apiKeyMapper) {
        this.apiKeyMapper = apiKeyMapper;
    }

    @Override
    public AuthVerifyResponse verify(String apiKey) {
        ApiKey key = apiKeyMapper.selectById(apiKey);
        if (key == null) {
            return new AuthVerifyResponse(false, null);
        }
        return new AuthVerifyResponse(true, key.getLabel(), key.getAvatarUrl());
    }
}
