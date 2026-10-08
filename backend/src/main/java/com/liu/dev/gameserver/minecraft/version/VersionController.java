package com.liu.dev.gameserver.minecraft.version;

import com.liu.dev.common.ApiResponse;
import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.java.JavaRequirement;
import com.liu.dev.gameserver.minecraft.version.provider.ProviderRegistry;
import com.liu.dev.gameserver.minecraft.version.provider.ServerProvider;
import com.liu.dev.gameserver.support.task.TaskService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** 版本查詢與版本庫（下載 / 刪除）API。 */
@RestController
@RequestMapping("/api/minecraft")
@PreAuthorize("hasRole('ADMIN')")
public class VersionController {

    /** 伺服器類型說明，供前端選單使用。 */
    public record TypeInfo(ServerType type, String name, String contentKind) {}

    /** 某個遊戲版本的資訊（含需要的 Java 版本）。 */
    public record VersionView(String id, String kind, String releasedAt, int requiredJava) {}

    public record LibraryRequest(ServerType type, String mcVersion, String build) {}

    private final ProviderRegistry providers;
    private final LibraryService library;
    private final TaskService tasks;

    public VersionController(ProviderRegistry providers, LibraryService library, TaskService tasks) {
        this.providers = providers;
        this.library = library;
        this.tasks = tasks;
    }

    @GetMapping("/types")
    public ApiResponse<List<TypeInfo>> types() {
        return ApiResponse.ok(Arrays.stream(ServerType.values())
                .map(t -> new TypeInfo(t, t.displayName(), t.contentKind().name())).toList());
    }

    @GetMapping("/versions")
    public ApiResponse<List<VersionView>> versions(@RequestParam ServerType type) {
        ServerProvider p = providers.get(type);
        return ApiResponse.ok(p.versions().stream()
                .map(v -> new VersionView(v.id(), v.kind(), v.releasedAt(), JavaRequirement.forMinecraft(v.id())))
                .toList());
    }

    @GetMapping("/versions/builds")
    public ApiResponse<List<BuildOption>> builds(@RequestParam ServerType type, @RequestParam String mcVersion) {
        return ApiResponse.ok(providers.get(type).builds(mcVersion));
    }

    @GetMapping("/library")
    public ApiResponse<List<LibraryService.Entry>> library() {
        return ApiResponse.ok(library.list());
    }

    @PostMapping("/library/download")
    public ApiResponse<Map<String, String>> download(@RequestBody LibraryRequest req) {
        if (req.type() == null || req.mcVersion() == null) throw new BusinessException("請選擇類型與版本");
        LibraryService.validateNames(req.mcVersion(), req.build());
        String taskId = tasks.submit("下載 " + req.type().displayName() + " " + req.mcVersion(),
                ctx -> library.ensureDownloaded(req.type(), req.mcVersion(), req.build(), ctx, 2, 98));
        return ApiResponse.ok(Map.of("taskId", taskId));
    }

    @PostMapping("/library/delete")
    public ApiResponse<Void> delete(@RequestBody LibraryRequest req) {
        if (req.type() == null || req.mcVersion() == null || req.build() == null) throw new BusinessException("參數不完整");
        library.delete(req.type(), req.mcVersion(), req.build());
        return ApiResponse.ok("已從版本庫刪除", null);
    }
}
