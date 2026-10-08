package com.liu.dev.gameserver.minecraft.settings;

import com.liu.dev.common.ApiResponse;
import com.liu.dev.gameserver.minecraft.server.McServer;
import com.liu.dev.gameserver.minecraft.server.McServerService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** server.properties 讀寫 API。 */
@RestController
@RequestMapping("/api/minecraft/servers/{id}/properties")
@PreAuthorize("hasRole('ADMIN')")
public class ServerSettingsController {

    /** values：要更新的 key → value。 */
    public record UpdateRequest(Map<String, String> values) {}

    private final McServerService servers;
    private final ServerPropertiesService properties;

    public ServerSettingsController(McServerService servers, ServerPropertiesService properties) {
        this.servers = servers;
        this.properties = properties;
    }

    @GetMapping
    public ApiResponse<Map<String, String>> read(@PathVariable long id) {
        McServer s = servers.require(id);
        return ApiResponse.ok(properties.read(s.getDirName()));
    }

    @PostMapping
    public ApiResponse<Void> update(@PathVariable long id, @RequestBody UpdateRequest req) {
        McServer s = servers.require(id);
        if (req.values() == null || req.values().isEmpty()) return ApiResponse.ok("沒有需要儲存的變更", null);
        properties.update(s.getDirName(), req.values());
        return ApiResponse.ok("server.properties 已儲存（需重新啟動後生效）", null);
    }
}
