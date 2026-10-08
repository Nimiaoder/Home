package com.liu.dev.gameserver.support.task;

import com.liu.dev.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 查詢背景工作進度：GET /api/tasks/{id}；重新整理後找回進行中的工作：GET /api/tasks/active?scope= */
@RestController
@RequestMapping("/api/tasks")
@PreAuthorize("hasRole('ADMIN')")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService tasks;

    public TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping("/active")
    public ApiResponse<List<TaskService.TaskView>> active(@RequestParam(required = false) String scope) {
        log.debug("*****TaskController.active*****");
        return ApiResponse.ok(tasks.running(scope));
    }

    @GetMapping("/{id}")
    public ApiResponse<TaskService.TaskView> get(@PathVariable String id) {
        log.debug("*****TaskController.get*****");
        return tasks.find(id)
                .map(ApiResponse::ok)
                .orElseGet(() -> ApiResponse.fail("找不到這個工作（可能已過期）"));
    }
}
