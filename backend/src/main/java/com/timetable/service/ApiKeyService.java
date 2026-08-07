package com.timetable.service;

import com.timetable.dto.AuthVerifyResponse;

public interface ApiKeyService {
    AuthVerifyResponse verify(String apiKey);
}
