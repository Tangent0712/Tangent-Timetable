package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.TodoRequest;
import com.timetable.entity.Todo;
import com.timetable.interceptor.RequestContext;
import com.timetable.service.AiService;
import com.timetable.service.impl.AiServiceImpl;
import com.timetable.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;
    private final AiService aiService;

    public TodoController(TodoService todoService, AiService aiService) {
        this.todoService = todoService;
        this.aiService = aiService;
    }

    @GetMapping
    public ApiResponse<List<Todo>> list() {
        return ApiResponse.success(todoService.list(RequestContext.getApiKey()));
    }

    @PostMapping
    public ApiResponse<Todo> create(@Valid @RequestBody TodoRequest request) {
        String apiKey = RequestContext.getApiKey();
        Todo todo = todoService.create(request, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_TODO);
        return ApiResponse.success(todo);
    }

    @PutMapping("/{id}")
    public ApiResponse<Todo> update(@PathVariable Long id, @Valid @RequestBody TodoRequest request) {
        String apiKey = RequestContext.getApiKey();
        Todo todo = todoService.update(id, request, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_TODO);
        return ApiResponse.success(todo);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        String apiKey = RequestContext.getApiKey();
        todoService.delete(id, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_TODO);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/toggle")
    public ApiResponse<Todo> toggleComplete(@PathVariable Long id) {
        String apiKey = RequestContext.getApiKey();
        Todo todo = todoService.toggleComplete(id, apiKey);
        aiService.invalidateProposalsOnDataChange(apiKey, AiServiceImpl.SCOPE_TODO);
        return ApiResponse.success(todo);
    }
}
