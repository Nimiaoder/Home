package com.liu.dev.gameserver.minecraft.runtime;

import com.liu.dev.common.ApiResponse;
import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.server.McServer;
import com.liu.dev.gameserver.minecraft.server.McServerService;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.support.download.DownloadService;
import com.liu.dev.gameserver.support.download.DownloadSource;
import com.liu.dev.gameserver.support.download.DownloadTicket;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 啟動 / 停止 / 重啟、狀態、主控台（SSE 串流 + 指令）。 */
@RestController
@RequestMapping("/api/minecraft/servers/{id}")
@PreAuthorize("hasRole('ADMIN')")
public class McRuntimeController {

    public record PowerRequest(String action) {}

    public record CommandRequest(String command) {}

    private final McServerService servers;
    private final McProcessManager processes;
    private final ConsoleHub hub;
    private final DownloadService downloads;
    private final MinecraftPaths paths;

    public McRuntimeController(McServerService servers, McProcessManager processes, ConsoleHub hub,
                               DownloadService downloads, MinecraftPaths paths) {
        this.servers = servers;
        this.processes = processes;
        this.hub = hub;
        this.downloads = downloads;
        this.paths = paths;
    }

    @GetMapping("/status")
    public ApiResponse<RuntimeStatus> status(@PathVariable long id) {
        servers.require(id);
        return ApiResponse.ok(processes.status(id));
    }

    /** action：start / stop / restart / kill */
    @PostMapping("/power")
    public ApiResponse<RuntimeStatus> power(@PathVariable long id, @RequestBody PowerRequest req) {
        McServer s = servers.require(id);
        String action = req.action() == null ? "" : req.action().toLowerCase();
        switch (action) {
            case "start" -> processes.start(s);
            case "stop" -> processes.stop(s);
            case "restart" -> processes.restart(s);
            case "kill" -> processes.kill(s);
            default -> throw new BusinessException("不支援的操作：" + req.action());
        }
        return ApiResponse.ok(processes.status(id));
    }

    @PostMapping("/console/command")
    public ApiResponse<Void> command(@PathVariable long id, @RequestBody CommandRequest req) {
        processes.sendCommand(servers.require(id), req.command());
        return ApiResponse.ok();
    }

    /**
     * 主控台即時串流（Server-Sent Events）。
     * 事件：hello（epoch）、lines（一批行）、status（狀態變更）。after 為前端已收到的最後 seq。
     */
    @GetMapping(value = "/console/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable long id,
                             @RequestParam(defaultValue = "0") long after,
                             @RequestParam(defaultValue = "0") long epoch,
                             HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");          // 避免 Nginx / Synology 反向代理緩衝，造成延遲
        try {
            McServer s = servers.require(id);
            return hub.channelFor(s).subscribe(after, epoch, processes.status(id));
        } catch (BusinessException e) {
            // 串流端點不能回 JSON（Accept 是 text/event-stream），改用 fatal 事件通知前端
            SseEmitter em = new SseEmitter(0L);
            try {
                em.send(SseEmitter.event().name("fatal").data(e.getMessage()));
            } catch (IOException ignored) {
                // 連線已斷
            }
            em.complete();
            return em;
        }
    }

    /** 下載 logs/latest.log。 */
    @PostMapping("/logs/download")
    public ApiResponse<DownloadTicket> downloadLog(@PathVariable long id) {
        McServer s = servers.require(id);
        Path log = paths.serverDir(s.getDirName()).resolve("logs").resolve("latest.log");
        if (!Files.isRegularFile(log)) throw new BusinessException("目前沒有 latest.log（伺服器還沒啟動過）");
        DownloadSource src = DownloadSource.ofFile(log, s.getName() + "-latest.log");
        return ApiResponse.ok(new DownloadTicket(downloads.issue(src), src.fileName()));
    }
}
