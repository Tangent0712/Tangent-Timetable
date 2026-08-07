package com.timetable.controller;

import com.timetable.dto.ApiResponse;
import com.timetable.dto.TodoRequest;
import com.timetable.entity.Todo;
import com.timetable.interceptor.RequestContext;
import com.timetable.service.TodoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @GetMapping
    public ApiResponse<List<Todo>> list() {
        return ApiResponse.success(todoService.list(RequestContext.getApiKey()));
    }

    @PostMapping
    public ApiResponse<Todo> create(@Valid @RequestBody TodoRequest request) {
        return ApiResponse.success(todoService.create(request, RequestContext.getApiKey()));
    }

    @PutMapping("/{id}")
    public ApiResponse<Todo> update(@PathVariable Long id, @Valid @RequestBody TodoRequest request) {
        return ApiResponse.success(todoService.update(id, request, RequestContext.getApiKey()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        todoService.delete(id, RequestContext.getApiKey());
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/toggle")
    public ApiResponse<Todo> toggleComplete(@PathVariable Long id) {
        return ApiResponse.success(todoService.toggleComplete(id, RequestContext.getApiKey()));
    }
}
