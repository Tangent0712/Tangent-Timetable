package com.timetable.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.timetable.dto.TodoRequest;
import com.timetable.entity.Todo;
import com.timetable.exception.BusinessException;
import com.timetable.mapper.TodoMapper;
import com.timetable.service.TodoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TodoServiceImpl implements TodoService {

    private final TodoMapper todoMapper;

    public TodoServiceImpl(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    @Override
    public List<Todo> list(String apiKey) {
        LambdaQueryWrapper<Todo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Todo::getApiKey, apiKey).orderByAsc(Todo::getDdl);
        return todoMapper.selectList(wrapper);
    }

    @Override
    public Todo create(TodoRequest request, String apiKey) {
        Todo todo = new Todo();
        todo.setApiKey(apiKey);
        todo.setTitle(request.getTitle());
        todo.setDdl(request.getDdl());
        todo.setCompleted(false);
        todoMapper.insert(todo);
        return todo;
    }

    @Override
    public Todo update(Long id, TodoRequest request, String apiKey) {
        Todo todo = todoMapper.selectById(id);
        if (todo == null || !todo.getApiKey().equals(apiKey)) {
            throw new BusinessException(404, "待办不存在");
        }
        todo.setTitle(request.getTitle());
        todo.setDdl(request.getDdl());
        todoMapper.updateById(todo);
        return todo;
    }

    @Override
    public void delete(Long id, String apiKey) {
        Todo todo = todoMapper.selectById(id);
        if (todo == null || !todo.getApiKey().equals(apiKey)) {
            throw new BusinessException(404, "待办不存在");
        }
        todoMapper.deleteById(id);
    }

    @Override
    public Todo toggleComplete(Long id, String apiKey) {
        Todo todo = todoMapper.selectById(id);
        if (todo == null || !todo.getApiKey().equals(apiKey)) {
            throw new BusinessException(404, "待办不存在");
        }
        todo.setCompleted(!Boolean.TRUE.equals(todo.getCompleted()));
        todoMapper.updateById(todo);
        return todo;
    }
}
