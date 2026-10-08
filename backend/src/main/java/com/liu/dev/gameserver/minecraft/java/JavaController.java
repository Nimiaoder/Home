package com.liu.dev.gameserver.minecraft.java;

import com.liu.dev.common.ApiResponse;
import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.support.task.TaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Java 環境 API：列出已偵測的 Java、下載安裝、移除。 */
@RestController
@RequestMapping("/api/minecraft/java")
@PreAuthorize("hasRole('ADMIN')")
public class JavaController {

    public record InstallRequest(Integer major) {}

    public record RemoveRequest(String path) {}

    private final JavaRuntimeService service;
    private final TaskService tasks;

    public JavaController(JavaRuntimeService service, TaskService tasks) {
        this.service = service;
        this.tasks = tasks;
    }

    @GetMapping
    public ApiResponse<List<JavaRuntimeService.JavaInfo>> list(@RequestParam(defaultValue = "false") boolean refresh) {
        return ApiResponse.ok(service.list(refresh));
    }

    @PostMapping("/install")
    public ApiResponse<Map<String, String>> install(@RequestBody InstallRequest req) {
        if (req.major() == null) throw new BusinessException("請選擇 Java 版本");
        int major = req.major();
        String taskId = tasks.submit("安裝 Java " + major, ctx -> service.install(major, ctx));
        return ApiResponse.ok(Map.of("taskId", taskId));
    }

    @PostMapping("/remove")
    public ApiResponse<Void> remove(@RequestBody RemoveRequest req) {
        service.remove(req.path());
        return ApiResponse.ok("已移除", null);
    }
}
