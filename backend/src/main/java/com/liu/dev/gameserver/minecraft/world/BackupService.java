package com.liu.dev.gameserver.minecraft.world;

import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.runtime.ConsoleChannel;
import com.liu.dev.gameserver.minecraft.runtime.ConsoleHub;
import com.liu.dev.gameserver.minecraft.runtime.McProcessManager;
import com.liu.dev.gameserver.minecraft.server.McServer;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.support.download.DownloadSource;
import com.liu.dev.gameserver.support.io.FileTool;
import com.liu.dev.gameserver.support.io.ZipTool;
import com.liu.dev.gameserver.support.path.SafePaths;
import com.liu.dev.gameserver.support.task.TaskService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** 地圖備份（zip，存放在 backups/{dirName}/）。伺服器執行中也能備份（會先 save-off + save-all flush）。 */
@Service
public class BackupService {

    private static final Pattern NAME = Pattern.compile("^(.+)__(\\d{8}-\\d{6})(?:__(.+))?\\.zip$");
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    /** 一份備份。tag：manual 以外的自動備份標記（before-restore / before-version-change）。 */
    public record BackupInfo(String fileName, String world, String tag, long sizeBytes, long createdAt) {}

    private final MinecraftPaths paths;
    private final WorldService worlds;
    private final McProcessManager processes;
    private final ConsoleHub hub;

    public BackupService(MinecraftPaths paths, WorldService worlds, McProcessManager processes, ConsoleHub hub) {
        this.paths = paths;
        this.worlds = worlds;
        this.processes = processes;
        this.hub = hub;
    }

    public List<BackupInfo> list(McServer s) {
        Path dir = paths.backupDir(s.getDirName());
        List<BackupInfo> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(Files::isRegularFile).toList()) {
                Matcher m = NAME.matcher(f.getFileName().toString());
                if (!m.matches()) continue;
                out.add(new BackupInfo(f.getFileName().toString(), m.group(1), m.group(3),
                        Files.size(f), Files.getLastModifiedTime(f).toMillis()));
            }
        } catch (IOException e) {
            throw new BusinessException("讀取備份清單失敗：" + FileTool.msg(e));
        }
        out.sort(Comparator.comparingLong(BackupInfo::createdAt).reversed());
        return out;
    }

    /** 備份指定地圖。tag 為 null 代表手動備份。 */
    public BackupInfo backup(McServer s, String rawWorld, String tag, TaskService.Context ctx) {
        String world = worlds.requireWorld(s, rawWorld);
        Path serverDir = paths.serverDir(s.getDirName());
        List<String> members = worlds.members(serverDir, world);
        Path dir = paths.backupDir(s.getDirName());
        String fileName = world + "__" + LocalDateTime.now().format(STAMP) + (tag == null ? "" : "__" + tag) + ".zip";
        Path target = dir.resolve(fileName);
        Path part = dir.resolve(fileName + ".part");

        boolean running = processes.isRunning(s.getId());
        try {
            Files.createDirectories(dir);
            if (running) {
                ctx.progress(-1, "通知伺服器儲存地圖…");
                ConsoleChannel ch = hub.channelFor(s);
                long seq = ch.lastSeq();
                processes.sendInternal(s, "save-off");
                processes.sendInternal(s, "save-all flush");
                ch.awaitMatch(seq, t -> t.contains("Saved the game") || t.contains("Saved the world"), 30_000);
            }
            ctx.progress(-1, "壓縮地圖「" + world + "」…");
            try (OutputStream out = Files.newOutputStream(part)) {
                ZipTool.zipDirs(serverDir, members, out);
            }
            FileTool.moveReplace(part, target);
            return new BackupInfo(fileName, world, tag, Files.size(target), Files.getLastModifiedTime(target).toMillis());
        } catch (IOException e) {
            try {
                Files.deleteIfExists(part);
            } catch (IOException ignored) {
                // ignore
            }
            throw new BusinessException("備份失敗：" + FileTool.msg(e));
        } finally {
            if (running) processes.sendInternal(s, "save-on");
        }
    }

    /** 備份目前使用中的地圖；地圖還不存在（伺服器沒跑過）就略過。 */
    public void backupActiveWorld(McServer s, String tag) {
        String world = worlds.activeWorldName(s);
        if (!Files.isRegularFile(paths.serverDir(s.getDirName()).resolve(world).resolve("level.dat"))) return;
        backup(s, world, tag, new TaskService.Context() {
            @Override
            public void progress(int percent, String message) {
            }

            @Override
            public void message(String message) {
            }
        });
    }

    /** 還原：先自動備份目前的地圖（before-restore），再用備份內容覆蓋。 */
    public void restore(McServer s, String rawFile, TaskService.Context ctx) {
        if (processes.isRunning(s.getId())) throw new BusinessException("請先停止伺服器再還原備份");
        Path zip = resolveBackup(s, rawFile);
        Matcher m = NAME.matcher(zip.getFileName().toString());
        if (!m.matches()) throw new BusinessException("備份檔名格式不正確");
        String world = m.group(1);
        Path serverDir = paths.serverDir(s.getDirName());
        if (Files.isRegularFile(serverDir.resolve(world).resolve("level.dat"))) {
            ctx.progress(-1, "還原前先備份目前的地圖…");
            backup(s, world, "before-restore", ctx);
        }
        worlds.importZip(s, zip, world, world, true, ctx);
    }

    public void delete(McServer s, String rawFile) {
        Path zip = resolveBackup(s, rawFile);
        try {
            Files.deleteIfExists(zip);
        } catch (IOException e) {
            throw new BusinessException("刪除備份失敗：" + FileTool.msg(e));
        }
    }

    public DownloadSource downloadSource(McServer s, String rawFile) {
        Path zip = resolveBackup(s, rawFile);
        return DownloadSource.ofFile(zip, zip.getFileName().toString());
    }

    private Path resolveBackup(McServer s, String rawFile) {
        String name = SafePaths.requireName(rawFile, "備份檔名");
        Path zip = SafePaths.resolveInside(paths.backupDir(s.getDirName()), name);
        if (!name.endsWith(".zip") || !Files.isRegularFile(zip)) throw new BusinessException("找不到這份備份");
        return zip;
    }
}
