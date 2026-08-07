package com.timetable.service;

import com.timetable.dto.TodoRequest;
import com.timetable.entity.Todo;

import java.util.List;

public interface TodoService {
    List<Todo> list(String apiKey);
    Todo create(TodoRequest request, String apiKey);
    Todo update(Long id, TodoRequest request, String apiKey);
    void delete(Long id, String apiKey);
    Todo toggleComplete(Long id, String apiKey);
}
