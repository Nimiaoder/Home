package com.liu.dev.gameserver.minecraft.world;

import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.runtime.McProcessManager;
import com.liu.dev.gameserver.minecraft.server.McServer;
import com.liu.dev.gameserver.minecraft.settings.ServerPropertiesService;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.support.download.DownloadSource;
import com.liu.dev.gameserver.support.io.FileTool;
import com.liu.dev.gameserver.support.io.ZipTool;
import com.liu.dev.gameserver.support.path.SafePaths;
import com.liu.dev.gameserver.support.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/**
 * 地圖管理。地圖 = 伺服器資料夾底下含有 level.dat 的資料夾。
 * Bukkit 系（Paper）的下界與終界會放在同層的 {name}_nether、{name}_the_end，
 * 匯出、備份、刪除時都視為同一張地圖一起處理。
 */
@Service
public class WorldService {

    private static final Logger log = LoggerFactory.getLogger(WorldService.class);

    private static final String[] SIBLING_SUFFIXES = {"_nether", "_the_end"};

    /** 地圖資訊。 */
    public record WorldInfo(String name, long sizeBytes, long modifiedAt, boolean active,
                            boolean hasNether, boolean hasEnd) {}

    private final MinecraftPaths paths;
    private final ServerPropertiesService properties;
    private final McProcessManager processes;

    public WorldService(MinecraftPaths paths, ServerPropertiesService properties, McProcessManager processes) {
        this.paths = paths;
        this.properties = properties;
        this.processes = processes;
    }

    public String activeWorldName(McServer s) {
        return properties.get(s.getDirName(), "level-name", "world");
    }

    public List<WorldInfo> list(McServer s) {
        Path serverDir = paths.serverDir(s.getDirName());
        String active = activeWorldName(s);
        List<WorldInfo> out = new ArrayList<>();
        if (!Files.isDirectory(serverDir)) return out;
        try (Stream<Path> children = Files.list(serverDir)) {
            for (Path dir : children.filter(Files::isDirectory).toList()) {
                Path levelDat = dir.resolve("level.dat");
                if (!Files.isRegularFile(levelDat)) continue;
                String name = dir.getFileName().toString();
                long size = 0;
                for (String m : members(serverDir, name)) size += FileTool.dirSize(serverDir.resolve(m));
                long mod = 0;
                try {
                    mod = Files.getLastModifiedTime(levelDat).toMillis();
                } catch (IOException ignored) {
                    // 顯示 0 即可
                }
                out.add(new WorldInfo(name, size, mod, name.equals(active),
                        Files.isDirectory(serverDir.resolve(name + "_nether")) || Files.isDirectory(dir.resolve("DIM-1"))
                                || Files.isDirectory(dir.resolve("dimensions").resolve("minecraft").resolve("the_nether")),
                        Files.isDirectory(serverDir.resolve(name + "_the_end")) || Files.isDirectory(dir.resolve("DIM1"))
                                || Files.isDirectory(dir.resolve("dimensions").resolve("minecraft").resolve("the_end"))));
            }
        } catch (IOException e) {
            throw new BusinessException("讀取地圖清單失敗：" + FileTool.msg(e));
        }
        out.sort(Comparator.comparing(WorldInfo::active).reversed().thenComparing(WorldInfo::name));
        return out;
    }

    /** 這張地圖實際存在的資料夾（主資料夾 + 同層的下界 / 終界）。 */
    public List<String> members(Path serverDir, String name) {
        List<String> out = new ArrayList<>();
        if (Files.isDirectory(serverDir.resolve(name))) out.add(name);
        for (String suffix : SIBLING_SUFFIXES) {
            if (Files.isDirectory(serverDir.resolve(name + suffix))) out.add(name + suffix);
        }
        return out;
    }

    /** 確認地圖存在並回傳名稱。 */
    public String requireWorld(McServer s, String rawName) {
        String name = SafePaths.requireName(rawName, "地圖名稱");
        if (!Files.isRegularFile(paths.serverDir(s.getDirName()).resolve(name).resolve("level.dat"))) {
            throw new BusinessException("找不到地圖「" + name + "」");
        }
        return name;
    }

    public void activate(McServer s, String rawName) {
        String name = requireWorld(s, rawName);
        log.info("伺服器「{}」改用地圖 {}", s.getName(), name);
        properties.set(s.getDirName(), "level-name", name);
    }

    public void delete(McServer s, String rawName) {
        String name = requireWorld(s, rawName);
        if (processes.isRunning(s.getId())) throw new BusinessException("請先停止伺服器再刪除地圖");
        log.info("伺服器「{}」刪除地圖 {}", s.getName(), name);
        deleteMembers(s, name);
    }

    private void deleteMembers(McServer s, String name) {
        Path serverDir = paths.serverDir(s.getDirName());
        try {
            for (String m : members(serverDir, name)) FileTool.deleteRecursively(serverDir.resolve(m));
        } catch (IOException e) {
            throw new BusinessException("刪除地圖失敗：" + FileTool.msg(e));
        }
    }

    /** 即時壓縮成 zip 下載（不產生暫存檔）。 */
    public DownloadSource exportSource(McServer s, String rawName) {
        String name = requireWorld(s, rawName);
        Path serverDir = paths.serverDir(s.getDirName());
        List<String> members = members(serverDir, name);
        log.info("伺服器「{}」匯出地圖 {}", s.getName(), name);
        return new DownloadSource() {
            @Override
            public String fileName() {
                return name + ".zip";
            }

            @Override
            public String contentType() {
                return "application/zip";
            }

            @Override
            public void writeTo(OutputStream out) throws IOException {
                ZipTool.zipDirs(serverDir, members, out);
            }
        };
    }

    /**
     * 把 zip 匯入為地圖。
     *
     * @param requestedName 使用者指定的名稱（可為空：沿用 zip 內的資料夾名稱）
     * @param fallbackName  zip 根目錄就是地圖本身（沒有外層資料夾）時使用的名稱，通常是檔名
     * @param overwrite     true 會先刪除同名地圖（呼叫端需自行確認已備份、伺服器已停止）
     */
    public String importZip(McServer s, Path zip, String requestedName, String fallbackName,
                            boolean overwrite, TaskService.Context ctx) {
        log.info("正在匯入地圖{}.....至伺服器「{}」（覆蓋：{}）", zip, s.getName(), overwrite);
        Path serverDir = paths.serverDir(s.getDirName());
        List<Path> created = new ArrayList<>();
        try (ZipFile zf = ZipTool.open(zip)) {
            String root = ZipTool.findWorldRoot(zf);
            if (root == null) throw new BusinessException("壓縮檔內找不到 level.dat，這不是有效的 Minecraft 地圖");

            String originalName = null;
            String parentPrefix = "";
            if (!root.isEmpty()) {
                String trimmed = root.substring(0, root.length() - 1);
                int i = trimmed.lastIndexOf('/');
                parentPrefix = i < 0 ? "" : trimmed.substring(0, i + 1);
                originalName = trimmed.substring(i + 1);
            }
            String raw = (requestedName != null && !requestedName.isBlank()) ? requestedName
                    : originalName != null ? originalName : fallbackName;
            String name = SafePaths.requireName(raw, "地圖名稱");

            boolean exists = !members(serverDir, name).isEmpty();
            if (exists && !overwrite) throw new BusinessException("已經有同名的地圖「" + name + "」，請換一個名稱");
            if (exists) deleteMembers(s, name);

            ctx.progress(-1, "解壓縮地圖「" + name + "」…");
            Path main = serverDir.resolve(name);
            created.add(main);
            ZipTool.extractPrefix(zf, root, main);
            if (originalName != null) {
                for (String suffix : SIBLING_SUFFIXES) {
                    String prefix = parentPrefix + originalName + suffix + "/";
                    if (ZipTool.hasPrefix(zf, prefix)) {
                        Path sib = serverDir.resolve(name + suffix);
                        created.add(sib);
                        ZipTool.extractPrefix(zf, prefix, sib);
                    }
                }
            }
            return name;
        } catch (IOException e) {
            cleanup(created);
            throw new BusinessException("匯入地圖失敗：" + FileTool.msg(e));
        } catch (RuntimeException e) {
            cleanup(created);
            throw e;
        }
    }

    private void cleanup(List<Path> dirs) {
        for (Path d : dirs) {
            try {
                FileTool.deleteRecursively(d);
            } catch (IOException ignored) {
                // 盡力而為
            }
        }
    }
}
