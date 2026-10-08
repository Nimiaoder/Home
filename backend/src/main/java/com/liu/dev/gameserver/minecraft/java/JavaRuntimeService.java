package com.liu.dev.gameserver.minecraft.java;

import com.liu.dev.common.BusinessException;
import com.liu.dev.config.AppProperties;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.support.cache.TtlCache;
import com.liu.dev.gameserver.support.http.HttpFetcher;
import com.liu.dev.gameserver.support.io.FileTool;
import com.liu.dev.gameserver.support.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java 環境：偵測本機已安裝的 Java、依 Minecraft 版本自動挑選、並可從 Adoptium 下載 Temurin 到 {base}/java/。
 * （Minecraft 26.1 起需要 Java 25，而一般 NAS 上多半只有 Java 17/21，所以這個功能很重要。）
 */
@Service
public class JavaRuntimeService {

    private static final Logger log = LoggerFactory.getLogger(JavaRuntimeService.class);
    private static final Pattern VERSION = Pattern.compile("version \"(\\d+)(?:\\.(\\d+))?([^\"]*)\"");

    /** 偵測到的一個 Java。path 是 bin/java 的完整路徑。 */
    public record JavaInfo(String path, int major, String version, boolean managed) {}

    /** 實際決定要用的 Java。 */
    public record JavaChoice(String path, int major, String version) {}

    private final MinecraftPaths paths;
    private final AppProperties props;
    private final HttpFetcher http;
    private final TtlCache<String, List<JavaInfo>> cache = new TtlCache<>(60 * 1000L);

    public JavaRuntimeService(MinecraftPaths paths, AppProperties props, HttpFetcher http) {
        this.paths = paths;
        this.props = props;
        this.http = http;
    }

    public List<JavaInfo> list(boolean refresh) {
        if (refresh) cache.clear();
        return cache.get("list", this::scan);
    }

    // ---------------------------------------------------------------- 偵測

    private List<JavaInfo> scan() {
        Set<Path> exes = new LinkedHashSet<>();
        scanDir(paths.java(), 3, exes);
        List<String> dirs = props.minecraft() == null || props.minecraft().javaSearchDirs() == null
                ? List.of() : props.minecraft().javaSearchDirs();
        for (String d : dirs) {
            if (StringUtils.hasText(d)) scanDir(Paths.get(d.trim()), 4, exes);
        }
        addExe(exes, Paths.get(System.getProperty("java.home", ""), "bin", "java"));
        String javaHome = System.getenv("JAVA_HOME");
        if (StringUtils.hasText(javaHome)) addExe(exes, Paths.get(javaHome, "bin", "java"));
        String pathEnv = System.getenv("PATH");
        if (pathEnv != null) {
            for (String dir : pathEnv.split(File.pathSeparator)) {
                if (StringUtils.hasText(dir)) addExe(exes, Paths.get(dir, "java"));
            }
        }

        Map<Path, JavaInfo> byReal = new LinkedHashMap<>();
        for (Path exe : exes) {
            Path real;
            try {
                real = exe.toRealPath();
            } catch (IOException e) {
                continue;
            }
            if (byReal.containsKey(real)) continue;
            JavaInfo info = inspect(exe);
            if (info != null) byReal.put(real, info);
        }
        List<JavaInfo> out = new ArrayList<>(byReal.values());
        out.sort(Comparator.comparingInt(JavaInfo::major).thenComparing(JavaInfo::path));
        return out;
    }

    private void scanDir(Path dir, int depth, Set<Path> out) {
        if (depth < 0 || !Files.isDirectory(dir)) return;
        Path exe = dir.resolve("bin").resolve("java");
        if (Files.isRegularFile(exe) && Files.isExecutable(exe)) {
            out.add(exe);
            return;                                           // 找到 JDK 根目錄就不再往內找
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path c : ds) {
                if (Files.isDirectory(c)) scanDir(c, depth - 1, out);
            }
        } catch (IOException | SecurityException ignored) {
            // 沒權限的資料夾直接略過
        }
    }

    private void addExe(Set<Path> out, Path exe) {
        if (Files.isRegularFile(exe) && Files.isExecutable(exe)) out.add(exe);
    }

    /** 執行 java -version 取得版本；無法執行回傳 null。 */
    public JavaInfo inspect(Path exe) {
        try {
            Process p = new ProcessBuilder(exe.toString(), "-version").redirectErrorStream(true).start();
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return null;
            }
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Matcher m = VERSION.matcher(out);
            if (!m.find()) return null;
            int first = Integer.parseInt(m.group(1));
            int major = (first == 1 && m.group(2) != null) ? Integer.parseInt(m.group(2)) : first;
            String full = m.group(0).replaceAll("^version \"|\"$", "");
            return new JavaInfo(exe.toAbsolutePath().toString(), major, full,
                    exe.toAbsolutePath().normalize().startsWith(paths.java()));
        } catch (IOException e) {
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // ---------------------------------------------------------------- 選擇

    /**
     * 決定要用哪個 Java。
     *
     * @param preferredPath 使用者在伺服器設定指定的路徑（空白＝自動）
     * @param requiredMajor 這個 Minecraft 版本需要的最低 Java 版本
     */
    public JavaChoice resolve(String preferredPath, int requiredMajor) {
        if (StringUtils.hasText(preferredPath)) {
            Path p = Paths.get(preferredPath.trim());
            if (Files.isDirectory(p)) p = p.resolve("bin").resolve("java");
            JavaInfo info = Files.isRegularFile(p) ? inspect(p) : null;
            if (info == null) throw new BusinessException("指定的 Java 無法執行：" + preferredPath.trim());
            return new JavaChoice(info.path(), info.major(), info.version());
        }
        JavaInfo best = null;
        for (JavaInfo j : list(false)) {
            if (j.major() < requiredMajor) continue;
            if (best == null || j.major() < best.major()
                    || (j.major() == best.major() && j.managed() && !best.managed())) {
                best = j;
            }
        }
        if (best == null) {
            throw new BusinessException("找不到 Java " + requiredMajor + " 或更新的版本，請到「資源庫 → Java 環境」下載安裝");
        }
        return new JavaChoice(best.path(), best.major(), best.version());
    }

    // ---------------------------------------------------------------- 安裝

    /** 從 Adoptium 下載 Temurin JRE 到 {base}/java/temurin-{major}/。 */
    public void install(int major, TaskService.Context ctx) {
        if (major < 8 || major > 99) throw new BusinessException("不支援的 Java 版本：" + major);
        String arch = switch (System.getProperty("os.arch", "")) {
            case "amd64", "x86_64" -> "x64";
            case "aarch64", "arm64" -> "aarch64";
            default -> throw new BusinessException("不支援的 CPU 架構：" + System.getProperty("os.arch"));
        };
        Path dest = paths.java().resolve("temurin-" + major);
        if (Files.isRegularFile(dest.resolve("bin").resolve("java"))) {
            throw new BusinessException("Java " + major + " 已經安裝在 " + dest);
        }
        Path tgz = paths.tmp().resolve("temurin-" + major + ".tar.gz");
        try {
            Files.createDirectories(paths.tmp());
            String url = "https://api.adoptium.net/v3/binary/latest/" + major + "/ga/linux/" + arch + "/jre/hotspot/normal/eclipse";
            ctx.progress(1, "下載 Java " + major + "…");
            http.download(url, tgz, null, null, (done, total) -> ctx.progress(
                    total > 0 ? (int) (done * 85 / total) : -1,
                    "下載 Java " + major + "　" + String.format("%.0f MB", done / 1024.0 / 1024.0)));

            ctx.progress(88, "解壓縮…");
            FileTool.deleteRecursively(dest);
            Files.createDirectories(dest);
            Process p = new ProcessBuilder("tar", "-xzf", tgz.toString(), "-C", dest.toString(), "--strip-components=1")
                    .redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (p.waitFor() != 0) throw new BusinessException("解壓縮失敗：" + out.trim());
            if (!Files.isRegularFile(dest.resolve("bin").resolve("java"))) {
                throw new BusinessException("下載的檔案不包含 bin/java，安裝失敗");
            }
            dest.resolve("bin").resolve("java").toFile().setExecutable(true);
            cache.clear();
            ctx.progress(100, "Java " + major + " 安裝完成");
        } catch (IOException e) {
            cleanup(dest);
            throw new BusinessException("安裝 Java 失敗：" + FileTool.msg(e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            cleanup(dest);
            throw new BusinessException("安裝已被中斷");
        } catch (BusinessException e) {
            cleanup(dest);
            throw e;
        } finally {
            try {
                Files.deleteIfExists(tgz);
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    /** 刪除由本系統安裝的 Java（只允許刪除 {base}/java/ 底下的）。 */
    public void remove(String javaExePath) {
        Path exe = Paths.get(javaExePath).toAbsolutePath().normalize();
        Path root = paths.java().toAbsolutePath().normalize();
        if (!exe.startsWith(root)) throw new BusinessException("只能移除由本系統安裝的 Java");
        Path rel = root.relativize(exe);
        if (rel.getNameCount() < 1) throw new BusinessException("路徑不合法");
        try {
            FileTool.deleteRecursively(root.resolve(rel.getName(0)));
            cache.clear();
        } catch (IOException e) {
            throw new BusinessException("移除失敗：" + FileTool.msg(e));
        }
    }

    private void cleanup(Path dir) {
        try {
            FileTool.deleteRecursively(dir);
        } catch (IOException e) {
            log.warn("清除未完成的 Java 安裝失敗：{}", e.getMessage());
        }
    }
}
