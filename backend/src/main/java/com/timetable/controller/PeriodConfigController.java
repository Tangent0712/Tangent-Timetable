package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.PeriodConfigRequest;
import com.timetable.entity.PeriodConfig;
import com.timetable.service.PeriodConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/period-config")
public class PeriodConfigController {

    private final PeriodConfigService periodConfigService;

    public PeriodConfigController(PeriodConfigService periodConfigService) {
        this.periodConfigService = periodConfigService;
    }

    @GetMapping
    public ApiResponse<List<PeriodConfig>> list() {
        return ApiResponse.success(periodConfigService.list());
    }

    @PutMapping
    public ApiResponse<List<PeriodConfig>> update(@Valid @RequestBody PeriodConfigRequest request) {
        return ApiResponse.success(periodConfigService.update(request));
    }
}
