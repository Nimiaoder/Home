package com.liu.dev.gameserver.minecraft.mod;

import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.server.McServer;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.minecraft.version.ServerType;
import com.liu.dev.gameserver.support.io.FileTool;
import com.liu.dev.gameserver.support.path.SafePaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/** 模組 / 插件檔案管理（mods/ 或 plugins/）。停用的檔案會改名為 *.jar.disabled。 */
@Service
public class ModService {

    private static final Logger log = LoggerFactory.getLogger(ModService.class);

    private static final String DISABLED = ".disabled";

    /** 一個模組 / 插件檔案。 */
    public record ModFile(String fileName, boolean enabled, long sizeBytes, long modifiedAt,
                          String name, String version, String modId) {}

    private final MinecraftPaths paths;
    private final Map<String, ModMetadataReader.Meta> metaCache = new ConcurrentHashMap<>();

    public ModService(MinecraftPaths paths) {
        this.paths = paths;
    }

    /** 這個伺服器的模組 / 插件資料夾；Vanilla 不支援。 */
    public Path contentDir(McServer s) {
        ServerType t = s.getType();
        if (t.contentDir() == null) {
            throw new BusinessException("Vanilla 伺服器不支援模組或插件，請改用 Fabric / Forge / NeoForge / Paper");
        }
        return paths.serverDir(s.getDirName()).resolve(t.contentDir());
    }

    public List<ModFile> list(McServer s) {
        Path dir = contentDir(s);
        List<ModFile> out = new ArrayList<>();
        if (!Files.isDirectory(dir)) return out;
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(Files::isRegularFile).toList()) {
                String fn = f.getFileName().toString();
                boolean enabled = fn.toLowerCase().endsWith(".jar");
                if (!enabled && !fn.toLowerCase().endsWith(".jar" + DISABLED)) continue;
                long size = Files.size(f);
                long mod = Files.getLastModifiedTime(f).toMillis();
                ModMetadataReader.Meta meta = metaCache.computeIfAbsent(f + "|" + mod + "|" + size,
                        k -> ModMetadataReader.read(f));
                out.add(new ModFile(fn, enabled, size, mod, meta.name(), meta.version(), meta.id()));
            }
        } catch (IOException e) {
            throw new BusinessException("讀取清單失敗：" + FileTool.msg(e));
        }
        out.sort(Comparator.comparing((ModFile m) -> m.fileName().toLowerCase()));
        return out;
    }

    /** 已安裝模組的 id（小寫），供安裝相依項目時避免重複。 */
    public Set<String> installedIds(McServer s) {
        Set<String> ids = new HashSet<>();
        for (ModFile m : list(s)) {
            if (m.modId() != null) ids.add(m.modId().toLowerCase());
        }
        return ids;
    }

    /** 以串流方式儲存上傳的 jar。 */
    public void upload(McServer s, String filename, InputStream in) throws IOException {
        Path dir = contentDir(s);
        String fn = SafePaths.requireName(filename, "檔名");
        if (!fn.toLowerCase().endsWith(".jar")) throw new BusinessException("只能上傳 .jar 檔案");
        log.info("正在儲存上傳的 {} 至 {}", fn, dir);
        Files.createDirectories(dir);
        Files.createDirectories(paths.tmp());
        Path tmp = paths.tmp().resolve("mod-" + UUID.randomUUID() + ".jar");
        try {
            Files.copy(in, tmp);
            try (ZipFile ignored = new ZipFile(tmp.toFile())) {
                // 只確認是有效的 zip / jar
            } catch (IOException e) {
                throw new BusinessException("這不是有效的 jar 檔案");
            }
            Files.deleteIfExists(dir.resolve(fn + DISABLED));
            FileTool.moveReplace(tmp, dir.resolve(fn));
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public void toggle(McServer s, String rawFile, boolean enable) {
        Path f = resolve(s, rawFile);
        String fn = f.getFileName().toString();
        boolean isEnabled = fn.toLowerCase().endsWith(".jar");
        if (isEnabled == enable) return;
        Path target = enable
                ? f.resolveSibling(fn.substring(0, fn.length() - DISABLED.length()))
                : f.resolveSibling(fn + DISABLED);
        if (Files.exists(target)) throw new BusinessException("已經有同名的檔案：" + target.getFileName());
        log.info("{} {}", enable ? "啟用" : "停用", fn);
        try {
            Files.move(f, target);
        } catch (IOException e) {
            throw new BusinessException("操作失敗：" + FileTool.msg(e));
        }
    }

    public void delete(McServer s, String rawFile) {
        try {
            Path target = resolve(s, rawFile);
            log.info("刪除 {}", target);
            Files.delete(target);
        } catch (IOException e) {
            throw new BusinessException("刪除失敗：" + FileTool.msg(e));
        }
    }

    private Path resolve(McServer s, String rawFile) {
        String fn = SafePaths.requireName(rawFile, "檔名");
        Path f = SafePaths.resolveInside(contentDir(s), fn);
        if (!Files.isRegularFile(f)) throw new BusinessException("找不到檔案：" + fn);
        return f;
    }
}
