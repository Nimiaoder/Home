package com.liu.dev.gameserver.minecraft.world;

import com.liu.dev.common.ApiResponse;
import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.server.McServer;
import com.liu.dev.gameserver.minecraft.server.McServerService;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.support.download.DownloadService;
import com.liu.dev.gameserver.support.download.DownloadSource;
import com.liu.dev.gameserver.support.download.DownloadTicket;
import com.liu.dev.gameserver.support.io.FileTool;
import com.liu.dev.gameserver.support.task.TaskService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 地圖與備份 API。 */
@RestController
@RequestMapping("/api/minecraft/servers/{id}")
@PreAuthorize("hasRole('ADMIN')")
public class WorldController {

    public record NameRequest(String name) {}

    public record FileRequest(String file) {}

    private final McServerService servers;
    private final WorldService worlds;
    private final BackupService backups;
    private final TaskService tasks;
    private final DownloadService downloads;
    private final MinecraftPaths paths;

    public WorldController(McServerService servers, WorldService worlds, BackupService backups,
                           TaskService tasks, DownloadService downloads, MinecraftPaths paths) {
        this.servers = servers;
        this.worlds = worlds;
        this.backups = backups;
        this.tasks = tasks;
        this.downloads = downloads;
        this.paths = paths;
    }

    @GetMapping("/worlds")
    public ApiResponse<List<WorldService.WorldInfo>> list(@PathVariable long id) {
        return ApiResponse.ok(worlds.list(servers.require(id)));
    }

    /** 上傳地圖 zip：請求本體就是檔案內容（串流寫入磁碟，不佔記憶體）。 */
    @PostMapping("/worlds/upload")
    public ApiResponse<Map<String, String>> upload(@PathVariable long id, @RequestParam String filename,
                                                   @RequestParam(required = false) String name,
                                                   HttpServletRequest request) throws IOException {
        McServer s = servers.require(id);
        if (!filename.toLowerCase().endsWith(".zip")) throw new BusinessException("請上傳 .zip 格式的地圖壓縮檔");
        String base = filename.substring(0, filename.length() - 4);
        Files.createDirectories(paths.tmp());
        Path tmp = paths.tmp().resolve("world-" + UUID.randomUUID() + ".zip");
        try (InputStream in = request.getInputStream()) {
            Files.copy(in, tmp);
        } catch (IOException e) {
            Files.deleteIfExists(tmp);
            throw new BusinessException("上傳中斷：" + FileTool.msg(e));
        }
        if (Files.size(tmp) == 0) {
            Files.deleteIfExists(tmp);
            throw new BusinessException("收到的檔案是空的");
        }
        String taskId = tasks.submit("匯入地圖 " + filename, ctx -> {
            try {
                worlds.importZip(s, tmp, name, base, false, ctx);
            } finally {
                Files.deleteIfExists(tmp);
            }
        });
        return ApiResponse.ok(Map.of("taskId", taskId));
    }

    @PostMapping("/worlds/activate")
    public ApiResponse<Void> activate(@PathVariable long id, @RequestBody NameRequest req) {
        worlds.activate(servers.require(id), req.name());
        return ApiResponse.ok("已設為使用中的地圖（重新啟動後生效）", null);
    }

    @PostMapping("/worlds/delete")
    public ApiResponse<Void> delete(@PathVariable long id, @RequestBody NameRequest req) {
        worlds.delete(servers.require(id), req.name());
        return ApiResponse.ok("地圖已刪除", null);
    }

    @PostMapping("/worlds/export")
    public ApiResponse<DownloadTicket> export(@PathVariable long id, @RequestBody NameRequest req) {
        DownloadSource src = worlds.exportSource(servers.require(id), req.name());
        return ApiResponse.ok(new DownloadTicket(downloads.issue(src), src.fileName()));
    }

    @PostMapping("/worlds/backup")
    public ApiResponse<Map<String, String>> backup(@PathVariable long id, @RequestBody NameRequest req) {
        McServer s = servers.require(id);
        String world = worlds.requireWorld(s, req.name());
        String taskId = tasks.submit("備份地圖 " + world, ctx -> backups.backup(s, world, null, ctx));
        return ApiResponse.ok(Map.of("taskId", taskId));
    }

    @GetMapping("/backups")
    public ApiResponse<List<BackupService.BackupInfo>> backups(@PathVariable long id) {
        return ApiResponse.ok(backups.list(servers.require(id)));
    }

    @PostMapping("/backups/restore")
    public ApiResponse<Map<String, String>> restore(@PathVariable long id, @RequestBody FileRequest req) {
        McServer s = servers.require(id);
        String taskId = tasks.submit("還原備份 " + req.file(), ctx -> backups.restore(s, req.file(), ctx));
        return ApiResponse.ok(Map.of("taskId", taskId));
    }

    @PostMapping("/backups/delete")
    public ApiResponse<Void> deleteBackup(@PathVariable long id, @RequestBody FileRequest req) {
        backups.delete(servers.require(id), req.file());
        return ApiResponse.ok("備份已刪除", null);
    }

    @PostMapping("/backups/download")
    public ApiResponse<DownloadTicket> downloadBackup(@PathVariable long id, @RequestBody FileRequest req) {
        DownloadSource src = backups.downloadSource(servers.require(id), req.file());
        return ApiResponse.ok(new DownloadTicket(downloads.issue(src), src.fileName()));
    }
}
