package com.liu.dev.gameserver.minecraft.server;

import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.java.JavaRequirement;
import com.liu.dev.gameserver.minecraft.java.JavaRuntimeService;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.minecraft.version.InstallContext;
import com.liu.dev.gameserver.minecraft.version.LibraryService;
import com.liu.dev.gameserver.minecraft.version.ServerType;
import com.liu.dev.gameserver.minecraft.version.provider.ProviderRegistry;
import com.liu.dev.gameserver.minecraft.version.provider.ServerProvider;
import com.liu.dev.gameserver.minecraft.world.BackupService;
import com.liu.dev.gameserver.support.io.FileTool;
import com.liu.dev.gameserver.support.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 安裝 / 切換版本的流程：（備份）→ 版本庫下載 → 安裝到伺服器資料夾 → 更新資料庫與版本紀錄。
 * 全程在背景工作中執行，前端用 taskId 輪詢進度。
 */
@Service
public class McInstallService {

    private static final Logger log = LoggerFactory.getLogger(McInstallService.class);

    private final McServerRepository repo;
    private final McVersionLogRepository logs;
    private final ProviderRegistry providers;
    private final LibraryService library;
    private final JavaRuntimeService javaService;
    private final MinecraftPaths paths;
    private final TaskService tasks;
    private final BackupService backups;
    private final Map<Long, String> activeTasks = new ConcurrentHashMap<>();

    public McInstallService(McServerRepository repo, McVersionLogRepository logs, ProviderRegistry providers,
                            LibraryService library, JavaRuntimeService javaService, MinecraftPaths paths,
                            TaskService tasks, BackupService backups) {
        this.repo = repo;
        this.logs = logs;
        this.providers = providers;
        this.library = library;
        this.javaService = javaService;
        this.paths = paths;
        this.tasks = tasks;
        this.backups = backups;
    }

    /** 目前正在進行中的安裝工作 id；沒有則回傳 null。 */
    public String activeTaskId(long serverId) {
        String id = activeTasks.get(serverId);
        if (id == null) return null;
        boolean running = tasks.find(id).map(t -> "RUNNING".equals(t.status())).orElse(false);
        if (!running) activeTasks.remove(serverId);
        return running ? id : null;
    }

    /**
     * 開始安裝（或切換版本）。
     *
     * @param build       空白代表使用預設建置
     * @param backupFirst 切換前先備份使用中的地圖
     * @param initial     true 表示建立伺服器後的第一次安裝
     */
    public String installAsync(long serverId, ServerType type, String mcVersion, String build,
                               boolean backupFirst, boolean initial) {
        McServer s = repo.findById(serverId).orElseThrow(() -> new BusinessException("伺服器不存在"));
        if (activeTaskId(serverId) != null) throw new BusinessException("這個伺服器正在安裝中，請稍候");
        boolean wasReady = s.getInstallState() == InstallState.READY;
        s.setInstallState(InstallState.INSTALLING);
        s.setInstallError(null);
        repo.save(s);
        String title = "安裝 " + type.displayName() + " " + mcVersion;
        String taskId = tasks.submit(title, ctx -> run(serverId, type, mcVersion, build, backupFirst && wasReady, wasReady, initial, ctx));
        activeTasks.put(serverId, taskId);
        return taskId;
    }

    private void run(long serverId, ServerType type, String mcVersion, String build,
                     boolean backupFirst, boolean wasReady, boolean initial, TaskService.Context ctx) {
        boolean touched = false;
        try {
            McServer s = repo.findById(serverId).orElseThrow(() -> new BusinessException("伺服器已被刪除"));
            ServerProvider provider = providers.get(type);
            Path dir = paths.serverDir(s.getDirName());

            if (backupFirst) {
                ctx.progress(1, "備份目前使用中的地圖…");
                backups.backupActiveWorld(s, "before-version-change");
            }
            LibraryService.Cached cached = library.ensureDownloaded(type, mcVersion, build, ctx, 3, 70);

            String javaExe = null;
            if (provider.needsJavaToInstall()) {
                javaExe = javaService.resolve(s.getJavaPath(), JavaRequirement.forMinecraft(mcVersion)).path();
            }
            ctx.progress(72, "安裝中…");
            touched = true;
            provider.install(new InstallContext(dir, cached.file(), javaExe, line -> ctx.message(line)));

            McServer fresh = repo.findById(serverId).orElseThrow(() -> new BusinessException("伺服器已被刪除"));
            fresh.setType(type);
            fresh.setMcVersion(mcVersion);
            fresh.setBuild(cached.build());
            fresh.setInstallState(InstallState.READY);
            fresh.setInstallError(null);
            repo.save(fresh);

            McVersionLog entry = new McVersionLog();
            entry.setServerId(serverId);
            entry.setType(type);
            entry.setMcVersion(mcVersion);
            entry.setBuild(cached.build());
            entry.setAction(initial ? "INSTALL" : "CHANGE");
            logs.save(entry);
            ctx.progress(100, "安裝完成");
        } catch (Throwable e) {
            String msg = e instanceof BusinessException ? e.getMessage() : "未預期的錯誤：" + FileTool.msg(e);
            if (!(e instanceof BusinessException)) log.error("安裝伺服器 {} 失敗", serverId, e);
            markFailed(serverId, msg, touched, wasReady);
            if (e instanceof BusinessException be) throw be;
            throw new BusinessException(msg);
        }
    }

    private void markFailed(long serverId, String msg, boolean touched, boolean wasReady) {
        repo.findById(serverId).ifPresent(s -> {
            if (!touched && wasReady) {
                s.setInstallState(InstallState.READY);       // 還沒動到伺服器檔案，維持原本可用的狀態
                s.setInstallError(null);
            } else {
                s.setInstallState(InstallState.FAILED);
                s.setInstallError(msg.length() > 900 ? msg.substring(0, 900) : msg);
            }
            repo.save(s);
        });
    }
}
