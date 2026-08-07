package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.AuthVerifyResponse;
import com.timetable.service.ApiKeyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final ApiKeyService apiKeyService;

    public AuthController(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }

    @GetMapping("/verify")
    public ApiResponse<AuthVerifyResponse> verify(@RequestHeader("X-API-Key") String apiKey) {
        return ApiResponse.success(apiKeyService.verify(apiKey));
    }
}
