package com.liu.dev.gameserver.minecraft.version;

import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.minecraft.version.provider.ProviderRegistry;
import com.liu.dev.gameserver.minecraft.version.provider.ServerProvider;
import com.liu.dev.gameserver.support.http.HttpFetcher;
import com.liu.dev.gameserver.support.io.FileTool;
import com.liu.dev.gameserver.support.path.SafePaths;
import com.liu.dev.gameserver.support.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 版本庫：把各種核心下載一份放在 library/{TYPE}/{mc}/{build}/，所有伺服器共用，避免重複下載。
 * 伺服器安裝時是「複製」一份到自己的資料夾，所以刪除版本庫的檔案不會影響已建立的伺服器。
 */
@Service
public class LibraryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryService.class);

    /** 版本庫中的一筆檔案。 */
    public record Entry(ServerType type, String mcVersion, String build, String fileName, long sizeBytes, long modifiedAt) {}

    /** ensureDownloaded 的結果：實際使用的 build 與檔案位置。 */
    public record Cached(String build, Path file) {}

    private static final TaskService.Context NOOP = new TaskService.Context() {
        @Override
        public void progress(int percent, String message) {
        }

        @Override
        public void message(String message) {
        }
    };

    private final MinecraftPaths paths;
    private final ProviderRegistry providers;
    private final HttpFetcher http;

    public LibraryService(MinecraftPaths paths, ProviderRegistry providers, HttpFetcher http) {
        this.paths = paths;
        this.providers = providers;
        this.http = http;
    }

    public List<Entry> list() {
        List<Entry> out = new ArrayList<>();
        for (ServerType type : ServerType.values()) {
            Path typeDir = paths.library().resolve(type.name());
            for (Path mcDir : children(typeDir)) {
                for (Path buildDir : children(mcDir)) {
                    findFile(buildDir).ifPresent(f -> out.add(toEntry(type, mcDir, buildDir, f)));
                }
            }
        }
        out.sort(Comparator.comparing((Entry e) -> e.type().ordinal())
                .thenComparing((a, b) -> McVersions.compare(b.mcVersion(), a.mcVersion()))
                .thenComparing(Entry::build, Comparator.reverseOrder()));
        return out;
    }

    private Entry toEntry(ServerType type, Path mcDir, Path buildDir, Path file) {
        long size = 0;
        long mod = 0;
        try {
            size = Files.size(file);
            mod = Files.getLastModifiedTime(file).toMillis();
        } catch (IOException ignored) {
            // 取不到就顯示 0
        }
        return new Entry(type, mcDir.getFileName().toString(), buildDir.getFileName().toString(),
                file.getFileName().toString(), size, mod);
    }

    private static List<Path> children(Path dir) {
        if (!Files.isDirectory(dir)) return List.of();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(Files::isDirectory).toList();
        } catch (IOException e) {
            return List.of();
        }
    }

    private static Optional<Path> findFile(Path dir) {
        if (!Files.isDirectory(dir)) return Optional.empty();
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(f -> Files.isRegularFile(f) && !f.getFileName().toString().endsWith(".part")).findFirst();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public Optional<Path> findCached(ServerType type, String mcVersion, String build) {
        return findFile(paths.libraryDir(type.name(), mcVersion, build));
    }

    /** 確保版本庫有這個版本（沒有就下載）。build 為空會使用預設建置。ctx 可為 null。 */
    public Cached ensureDownloaded(ServerType type, String mcVersion, String build,
                                   TaskService.Context ctx, int fromPercent, int toPercent) {
        TaskService.Context c = ctx == null ? NOOP : ctx;
        ServerProvider provider = providers.get(type);
        String b = StringUtils.hasText(build) ? build : provider.defaultBuild(mcVersion);
        Optional<Path> cached = findCached(type, mcVersion, b);
        if (cached.isPresent()) return new Cached(b, cached.get());

        log.info("版本庫沒有 {} {}（{}），準備下載", type, mcVersion, b);
        c.progress(fromPercent, "解析 " + type.displayName() + " " + mcVersion + " 的下載位置…");
        Artifact a = provider.resolve(mcVersion, b);
        String fileName = safeFileName(a.fileName());
        Path target = paths.libraryDir(type.name(), mcVersion, b).resolve(fileName);
        c.progress(fromPercent, "下載 " + fileName);
        http.download(a.url(), target, a.hashAlgo(), a.hash(), (done, total) -> {
            c.transfer(done, total);
            int pct = total > 0 ? fromPercent + (int) ((toPercent - fromPercent) * done / total) : -1;
            c.progress(pct, null);
        });
        c.clearTransfer();
        return new Cached(b, target);
    }

    public void delete(ServerType type, String mcVersion, String build) {
        log.info("從版本庫刪除 {} {}（{}）", type, mcVersion, build);
        Path dir = paths.libraryDir(type.name(), mcVersion, build);
        if (!Files.isDirectory(dir)) throw new BusinessException("版本庫中沒有這個版本");
        try {
            FileTool.deleteRecursively(dir);
            Path parent = dir.getParent();                       // 順手清掉空的上層資料夾
            if (parent != null && children(parent).isEmpty() && findFile(parent).isEmpty()) Files.deleteIfExists(parent);
        } catch (IOException e) {
            throw new BusinessException("刪除失敗：" + FileTool.msg(e));
        }
    }

    private static String safeFileName(String name) {
        String n = name == null ? "" : Paths.get(name).getFileName().toString();
        n = n.replaceAll("[^\\p{L}\\p{N}_.+\\-]", "_");
        return n.isBlank() || n.startsWith(".") ? "server.jar" : n;
    }

    /** 驗證 mcVersion / build 可安全當成資料夾名稱（API 入口使用）。 */
    public static void validateNames(String mcVersion, String build) {
        SafePaths.requireName(mcVersion, "遊戲版本");
        if (StringUtils.hasText(build)) SafePaths.requireName(build, "建置版本");
    }
}
