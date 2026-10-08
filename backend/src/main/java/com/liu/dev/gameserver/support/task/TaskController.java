package com.liu.dev.gameserver.support.task;

import com.liu.dev.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 查詢背景工作進度：GET /api/tasks/{id} */
@RestController
@RequestMapping("/api/tasks")
@PreAuthorize("hasRole('ADMIN')")
public class TaskController {

    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskService.TaskView> get(@PathVariable String id) {
        return tasks.find(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("找不到這個工作（可能已過期）"));
    }
}
