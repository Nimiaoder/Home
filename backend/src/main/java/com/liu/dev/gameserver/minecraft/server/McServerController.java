package com.liu.dev.gameserver.minecraft.server;

import com.liu.dev.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 伺服器管理 API（僅管理員）。 */
@RestController
@RequestMapping("/api/minecraft/servers")
@PreAuthorize("hasRole('ADMIN')")
public class McServerController {

    private static final Logger log = LoggerFactory.getLogger(McServerController.class);

    private final McServerService service;

    public McServerController(McServerService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<McServerDto.View>> list() {
        log.debug("*****McServerController.list*****");
        return ApiResponse.ok(service.list());
    }

    @PostMapping
    public ApiResponse<McServerDto.View> create(@RequestBody McServerDto.CreateRequest req) {
        log.debug("*****McServerController.create*****");
        return ApiResponse.ok("伺服器已建立，正在下載並安裝…", service.create(req));
    }

    @GetMapping("/{id}")
    public ApiResponse<McServerDto.View> get(@PathVariable long id) {
        log.debug("*****McServerController.get*****");
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping("/{id}/update")
    public ApiResponse<McServerDto.View> update(@PathVariable long id, @RequestBody McServerDto.UpdateRequest req) {
        log.debug("*****McServerController.update*****");
        return ApiResponse.ok("已儲存（連接埠、記憶體等變更需重新啟動後生效）", service.update(id, req));
    }

    @PostMapping("/{id}/delete")
    public ApiResponse<Void> delete(@PathVariable long id, @RequestBody(required = false) McServerDto.DeleteRequest req) {
        log.debug("*****McServerController.delete*****");
        service.delete(id, req != null && Boolean.TRUE.equals(req.deleteFiles()));
        return ApiResponse.ok("伺服器已刪除", null);
    }

    @PostMapping("/{id}/eula/accept")
    public ApiResponse<Void> acceptEula(@PathVariable long id) {
        log.debug("*****McServerController.acceptEula*****");
        service.acceptEula(id);
        return ApiResponse.ok("已同意 Minecraft EULA", null);
    }

    @GetMapping("/{id}/history")
    public ApiResponse<List<McServerDto.VersionLogView>> history(@PathVariable long id) {
        log.debug("*****McServerController.history*****");
        return ApiResponse.ok(service.history(id));
    }

    @PostMapping("/{id}/version/change")
    public ApiResponse<Map<String, String>> changeVersion(@PathVariable long id,
                                                           @RequestBody McServerDto.ChangeVersionRequest req) {
        log.debug("*****McServerController.changeVersion*****");
        return ApiResponse.ok(Map.of("taskId", service.changeVersion(id, req)));
    }

    @PostMapping("/{id}/version/retry")
    public ApiResponse<Map<String, String>> retry(@PathVariable long id) {
        log.debug("*****McServerController.retry*****");
        return ApiResponse.ok(Map.of("taskId", service.retryInstall(id)));
    }
}
