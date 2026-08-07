package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.RecurringTodoRequest;
import com.timetable.entity.RecurringTodo;
import com.timetable.interceptor.RequestContext;
import com.timetable.service.AiService;
import com.timetable.service.RecurringTodoService;
import com.timetable.service.impl.AiServiceImpl;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-todos")
public class RecurringTodoController {

    private final RecurringTodoService recurringTodoService;
    private final AiService aiService;

    public RecurringTodoController(RecurringTodoService recurringTodoService, AiService aiService) {
        this.recurringTodoService = recurringTodoService;
        this.aiService = aiService;
    }

    @GetMapping
    public ApiResponse<List<RecurringTodo>> list() {
        return ApiResponse.success(recurringTodoService.list(RequestContext.getApiKey()));
    }

    @PostMapping
    public ApiResponse<RecurringTodo> create(@Valid @RequestBody RecurringTodoRequest request) {
        String apiKey = RequestContext.getApiKey();
        RecurringTodo rule = recurringTodoService.create(request, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_TODO);
        return ApiResponse.success(rule);
    }

    @PutMapping("/{id}")
    public ApiResponse<RecurringTodo> update(@PathVariable Long id,
                                             @Valid @RequestBody RecurringTodoRequest request) {
        String apiKey = RequestContext.getApiKey();
        RecurringTodo rule = recurringTodoService.update(id, request, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_TODO);
        return ApiResponse.success(rule);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String apiKey = RequestContext.getApiKey();
        recurringTodoService.delete(id, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_TODO);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/toggle")
    public ApiResponse<RecurringTodo> toggle(@PathVariable Long id) {
        String apiKey = RequestContext.getApiKey();
        RecurringTodo rule = recurringTodoService.toggleEnabled(id, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_TODO);
        return ApiResponse.success(rule);
    }

    @PostMapping("/{id}/trigger")
    public ApiResponse<RecurringTodo> triggerNow(@PathVariable Long id) {
        String apiKey = RequestContext.getApiKey();
        RecurringTodo rule = recurringTodoService.triggerNow(id, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_TODO);
        return ApiResponse.success(rule);
    }
}
