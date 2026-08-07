package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.RecurringTodoRequest;
import com.timetable.entity.RecurringTodo;
import com.timetable.interceptor.RequestContext;
import com.timetable.service.AiService;
import com.timetable.service.RecurringTodoService;
import com.timetable.service.impl.AiServiceImpl;
import com.timetable.service.impl.RecurringScriptEvaluator;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** 自定义脚本的模板与可用上下文字段，供前端编辑器展示 */
    @GetMapping("/script-template")
    public ApiResponse<Map<String, Object>> scriptTemplate() {
        Map<String, Object> data = new HashMap<>();
        data.put("template", RecurringScriptEvaluator.SCRIPT_TEMPLATE);
        data.put("maxLength", RecurringScriptEvaluator.MAX_SCRIPT_LENGTH);
        data.put("context", RecurringScriptEvaluator.buildContext(
                java.time.LocalDateTime.now(), null));
        return ApiResponse.success(data);
    }

    /** 试运行脚本：校验语法并返回此刻是否会触发 */
    @PostMapping("/test-script")
    public ApiResponse<Map<String, Object>> testScript(@RequestBody Map<String, String> body) {
        RecurringScriptEvaluator.ScriptResult r =
                recurringTodoService.testScript(body.get("script"));
        Map<String, Object> data = new HashMap<>();
        data.put("valid", r.ok());
        data.put("triggeredNow", r.triggered());
        data.put("error", r.error());
        return ApiResponse.success(data);
    }

    @PostMapping("/{id}/trigger")
    public ApiResponse<RecurringTodo> triggerNow(@PathVariable Long id) {
        String apiKey = RequestContext.getApiKey();
        RecurringTodo rule = recurringTodoService.triggerNow(id, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_TODO);
        return ApiResponse.success(rule);
    }
}
