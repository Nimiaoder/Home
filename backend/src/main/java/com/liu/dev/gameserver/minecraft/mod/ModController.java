package com.liu.dev.gameserver.minecraft.mod;

import com.liu.dev.common.ApiResponse;
import com.liu.dev.gameserver.minecraft.mod.modrinth.ModrinthService;
import com.liu.dev.gameserver.minecraft.server.McServer;
import com.liu.dev.gameserver.minecraft.server.McServerService;
import com.liu.dev.gameserver.support.io.FileTool;
import com.liu.dev.gameserver.support.task.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/** 模組 / 插件 API（本機檔案管理 + Modrinth 線上安裝）。 */
@RestController
@RequestMapping("/api/minecraft/servers/{id}/mods")
@PreAuthorize("hasRole('ADMIN')")
public class ModController {

    /** 列表回傳：資料夾種類與檔案。 */
    public record ModList(String contentKind, String contentDir, List<ModService.ModFile> files) {}

    public record ToggleRequest(String file, boolean enabled) {}

    public record FileRequest(String file) {}

    public record InstallRequest(String projectId) {}

    private final McServerService servers;
    private final ModService mods;
    private final ModrinthService modrinth;
    private final TaskService tasks;

    public ModController(McServerService servers, ModService mods, ModrinthService modrinth, TaskService tasks) {
        this.servers = servers;
        this.mods = mods;
        this.modrinth = modrinth;
        this.tasks = tasks;
    }

    @GetMapping
    public ApiResponse<ModList> list(@PathVariable long id) {
        McServer s = servers.require(id);
        return ApiResponse.ok(new ModList(s.getType().contentKind().name(), s.getType().contentDir(), mods.list(s)));
    }

    /** 請求本體即為 jar 檔內容。 */
    @PostMapping("/upload")
    public ApiResponse<Void> upload(@PathVariable long id, @RequestParam String filename,
                                    HttpServletRequest request) throws IOException {
        McServer s = servers.require(id);
        try (InputStream in = request.getInputStream()) {
            mods.upload(s, filename, in);
        }
        return ApiResponse.ok("已上傳 " + filename, null);
    }

    @PostMapping("/toggle")
    public ApiResponse<Void> toggle(@PathVariable long id, @RequestBody ToggleRequest req) {
        mods.toggle(servers.require(id), req.file(), req.enabled());
        return ApiResponse.ok(req.enabled() ? "已啟用（需重新啟動伺服器）" : "已停用（需重新啟動伺服器）", null);
    }

    @PostMapping("/delete")
    public ApiResponse<Void> delete(@PathVariable long id, @RequestBody FileRequest req) {
        mods.delete(servers.require(id), req.file());
        return ApiResponse.ok("已刪除", null);
    }

    @GetMapping("/search")
    public ApiResponse<ModrinthService.SearchResult> search(@PathVariable long id,
                                                            @RequestParam(defaultValue = "") String query,
                                                            @RequestParam(defaultValue = "0") int offset) {
        return ApiResponse.ok(modrinth.search(servers.require(id), query, offset));
    }

    @PostMapping("/install")
    public ApiResponse<Map<String, String>> install(@PathVariable long id, @RequestBody InstallRequest req) {
        McServer s = servers.require(id);
        String taskId = tasks.submit("安裝 " + req.projectId(), ctx -> modrinth.install(s, req.projectId(), ctx));
        return ApiResponse.ok(Map.of("taskId", taskId));
    }
}
