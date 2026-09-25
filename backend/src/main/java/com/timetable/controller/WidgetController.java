package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.exception.BusinessException;
import com.timetable.mapper.ApiKeyMapper;
import com.timetable.service.WidgetService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 小组件 / WebView 展示接口（只读，key 通过 URL 传入）。
 * 由 WebView 宿主（iPad 小组件、Android 全屏应用）加载，展示页每 60 秒轮询一次，
 * 每次请求都实时聚合最新数据返回。
 * 免鉴权拦截器（见 WebMvcConfig），key 在此手动校验。
 */
@RestController
@RequestMapping("/api/widget")
public class WidgetController {

    private final ApiKeyMapper apiKeyMapper;
    private final WidgetService widgetService;

    public WidgetController(ApiKeyMapper apiKeyMapper, WidgetService widgetService) {
        this.apiKeyMapper = apiKeyMapper;
        this.widgetService = widgetService;
    }

    @GetMapping(value = "/overview/{key}", produces = "application/json;charset=utf-8")
    public ApiResponse<Map<String, Object>> overview(
            @PathVariable String key,
            @RequestParam(value = "scheduleId", required = false) Long scheduleId,
            HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        if (key.isBlank()) {
            throw new BusinessException(400, "缺少 key 参数");
        }
        if (apiKeyMapper.selectById(key.trim()) == null) {
            throw new BusinessException(401, "API Key 无效");
        }
        return ApiResponse.success(widgetService.overview(key.trim(), scheduleId));
    }
}