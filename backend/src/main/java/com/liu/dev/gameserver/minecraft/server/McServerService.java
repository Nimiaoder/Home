package com.liu.dev.gameserver.minecraft.server;

import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.java.JavaRequirement;
import com.liu.dev.gameserver.minecraft.java.JavaRuntimeService;
import com.liu.dev.gameserver.minecraft.runtime.ConsoleHub;
import com.liu.dev.gameserver.minecraft.runtime.McProcessManager;
import com.liu.dev.gameserver.minecraft.settings.EulaFile;
import com.liu.dev.gameserver.minecraft.settings.ServerPropertiesService;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.minecraft.version.ServerType;
import com.liu.dev.gameserver.minecraft.version.provider.ProviderRegistry;
import com.liu.dev.gameserver.minecraft.version.provider.ServerProvider;
import com.liu.dev.gameserver.support.io.FileTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 伺服器的建立 / 修改 / 刪除 / 查詢。 */
@Service
public class McServerService {

    private static final Logger log = LoggerFactory.getLogger(McServerService.class);

    private static final int DEFAULT_PORT = 25565;

    private final McServerRepository repo;
    private final McVersionLogRepository logs;
    private final McProcessManager processes;
    private final McInstallService installer;
    private final ProviderRegistry providers;
    private final MinecraftPaths paths;
    private final ServerPropertiesService properties;
    private final JavaRuntimeService javaService;
    private final ConsoleHub hub;
    private final SecureRandom random = new SecureRandom();

    public McServerService(McServerRepository repo, McVersionLogRepository logs, McProcessManager processes,
                           McInstallService installer, ProviderRegistry providers, MinecraftPaths paths,
                           ServerPropertiesService properties, JavaRuntimeService javaService, ConsoleHub hub) {
        this.repo = repo;
        this.logs = logs;
        this.processes = processes;
        this.installer = installer;
        this.providers = providers;
        this.paths = paths;
        this.properties = properties;
        this.javaService = javaService;
        this.hub = hub;
    }

    // ---------------------------------------------------------------- 查詢

    public McServer require(long id) {
        return repo.findById(id).orElseThrow(() -> new BusinessException("伺服器不存在"));
    }

    public List<McServerDto.View> list() {
        return repo.findAll().stream().map(this::view).toList();
    }

    public McServerDto.View get(long id) {
        return view(require(id));
    }

    public List<McServerDto.VersionLogView> history(long id) {
        require(id);
        return logs.findTop50ByServerIdOrderByIdDesc(id).stream()
                .map(l -> new McServerDto.VersionLogView(l.getType(), l.getMcVersion(), l.getBuild(), l.getAction(),
                        l.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()))
                .toList();
    }

    public McServerDto.View view(McServer s) {
        Path dir = paths.serverDir(s.getDirName());
        ServerType t = s.getType();
        return new McServerDto.View(s.getId(), s.getName(), s.getDescription(), t, t.displayName(),
                s.getMcVersion(), s.getBuild(), s.getPort(), s.getMemoryMb(), s.getJavaPath(), s.getJvmArgs(),
                s.isAutoStart(), s.getInstallState(), s.getInstallError(), installer.activeTaskId(s.getId()),
                EulaFile.isAccepted(dir), t.contentKind().name(), t.contentDir(),
                JavaRequirement.forMinecraft(s.getMcVersion()), dir.toString(),
                processes.status(s.getId()),
                s.getCreatedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli());
    }

    // ---------------------------------------------------------------- 建立

    public McServerDto.View create(McServerDto.CreateRequest req) {
        String name = cleanName(req.name());
        if (repo.existsByNameIgnoreCase(name)) throw new BusinessException("已經有同名的伺服器");
        if (req.type() == null) throw new BusinessException("請選擇伺服器類型");
        if (!StringUtils.hasText(req.mcVersion())) throw new BusinessException("請選擇 Minecraft 版本");
        String mc = req.mcVersion().trim();
        ServerProvider provider = providers.get(req.type());
        if (provider.versions().stream().noneMatch(v -> v.id().equals(mc))) {
            throw new BusinessException(req.type().displayName() + " 沒有 Minecraft " + mc + " 這個版本");
        }
        int port = req.port() == null ? nextFreePort() : checkPort(req.port(), null);
        int memory = checkMemory(req.memoryMb() == null ? 2048 : req.memoryMb());
        String javaPath = cleanJava(req.javaPath());
        String jvmArgs = cleanJvmArgs(req.jvmArgs());

        McServer s = new McServer();
        s.setName(name);
        s.setDescription(cleanDescription(req.description()));
        s.setDirName(uniqueDirName(name));
        s.setType(req.type());
        s.setMcVersion(mc);
        s.setPort(port);
        s.setMemoryMb(memory);
        s.setJavaPath(javaPath);
        s.setJvmArgs(jvmArgs);
        s.setAutoStart(Boolean.TRUE.equals(req.autoStart()));
        s.setInstallState(InstallState.INSTALLING);

        Path dir = paths.serverDir(s.getDirName());
        try {
            Files.createDirectories(dir);
            properties.update(s.getDirName(), Map.of("server-port", String.valueOf(port), "motd", name));
            if (Boolean.TRUE.equals(req.acceptEula())) EulaFile.accept(dir);
        } catch (IOException e) {
            throw new BusinessException("無法建立伺服器資料夾 " + dir + "：" + FileTool.msg(e)
                    + "（請確認 " + paths.base() + " 存在且程式有寫入權限）");
        }
        s = repo.save(s);
        log.info("已建立伺服器「{}」（id={}，{} {}，連接埠 {}，記憶體 {} MB，目錄 {}）",
                s.getName(), s.getId(), req.type(), mc, port, memory, dir);
        installer.installAsync(s.getId(), req.type(), mc, req.build(), false, true);
        return view(repo.findById(s.getId()).orElse(s));
    }

    // ---------------------------------------------------------------- 修改

    public McServerDto.View update(long id, McServerDto.UpdateRequest req) {
        McServer s = require(id);
        if (req.name() != null) {
            String name = cleanName(req.name());
            if (!name.equalsIgnoreCase(s.getName()) && repo.existsByNameIgnoreCase(name)) {
                throw new BusinessException("已經有同名的伺服器");
            }
            s.setName(name);
        }
        if (req.description() != null) s.setDescription(cleanDescription(req.description()));
        if (req.port() != null) s.setPort(checkPort(req.port(), id));
        if (req.memoryMb() != null) s.setMemoryMb(checkMemory(req.memoryMb()));
        if (req.javaPath() != null) s.setJavaPath(cleanJava(req.javaPath()));
        if (req.jvmArgs() != null) s.setJvmArgs(cleanJvmArgs(req.jvmArgs()));
        if (req.autoStart() != null) s.setAutoStart(req.autoStart());
        repo.save(s);
        log.info("已更新伺服器「{}」（id={}）的設定", s.getName(), id);
        if (!processes.isRunning(id) && s.getInstallState() == InstallState.READY) {
            properties.set(s.getDirName(), "server-port", String.valueOf(s.getPort()));
        }
        return view(s);
    }

    public void acceptEula(long id) {
        McServer s = require(id);
        log.info("伺服器「{}」（id={}）同意 EULA", s.getName(), id);
        try {
            EulaFile.accept(paths.serverDir(s.getDirName()));
        } catch (IOException e) {
            throw new BusinessException("寫入 eula.txt 失敗：" + FileTool.msg(e));
        }
    }

    // ---------------------------------------------------------------- 切換版本

    public String changeVersion(long id, McServerDto.ChangeVersionRequest req) {
        McServer s = require(id);
        if (processes.isRunning(id)) throw new BusinessException("請先停止伺服器再切換版本");
        if (req.type() == null || !StringUtils.hasText(req.mcVersion())) throw new BusinessException("請選擇類型與版本");
        String mc = req.mcVersion().trim();
        ServerProvider provider = providers.get(req.type());
        if (provider.versions().stream().noneMatch(v -> v.id().equals(mc))) {
            throw new BusinessException(req.type().displayName() + " 沒有 Minecraft " + mc + " 這個版本");
        }
        log.info("伺服器「{}」（id={}）切換版本：{} {} {}", s.getName(), id, req.type(), mc, req.build());
        return installer.installAsync(s.getId(), req.type(), mc, req.build(), !Boolean.FALSE.equals(req.backupFirst()), false);
    }

    /** 安裝失敗後，以目前記錄的版本重新安裝。 */
    public String retryInstall(long id) {
        McServer s = require(id);
        log.info("伺服器「{}」（id={}）重新安裝 {} {}", s.getName(), id, s.getType(), s.getMcVersion());
        return installer.installAsync(id, s.getType(), s.getMcVersion(), s.getBuild(), false, true);
    }

    // ---------------------------------------------------------------- 刪除

    public void delete(long id, boolean deleteFiles) {
        McServer s = require(id);
        if (processes.isRunning(id)) throw new BusinessException("請先停止伺服器再刪除");
        if (installer.activeTaskId(id) != null) throw new BusinessException("伺服器正在安裝中，請稍後再刪除");
        log.info("刪除伺服器「{}」（id={}），同時刪除檔案：{}", s.getName(), id, deleteFiles);
        hub.remove(id);
        logs.deleteByServerId(id);
        repo.delete(s);
        if (deleteFiles) {
            try {
                FileTool.deleteRecursively(paths.serverDir(s.getDirName()));
                FileTool.deleteRecursively(paths.backupDir(s.getDirName()));
            } catch (IOException e) {
                throw new BusinessException("伺服器已從清單移除，但刪除檔案時失敗：" + FileTool.msg(e));
            }
        }
    }

    // ---------------------------------------------------------------- 驗證

    private static String cleanName(String raw) {
        String n = raw == null ? "" : raw.trim();
        if (n.isEmpty() || n.length() > 50) throw new BusinessException("伺服器名稱需為 1～50 個字");
        return n;
    }

    private static String cleanDescription(String raw) {
        String d = raw == null ? "" : raw.trim();
        if (d.length() > 255) throw new BusinessException("說明最多 255 個字");
        return d.isEmpty() ? null : d;
    }

    private int checkPort(int port, Long exceptId) {
        if (port < 1024 || port > 65535) throw new BusinessException("連接埠需介於 1024～65535");
        boolean taken = repo.findAll().stream().anyMatch(o -> o.getPort() == port && !o.getId().equals(exceptId));
        if (taken) throw new BusinessException("連接埠 " + port + " 已被其他伺服器使用");
        return port;
    }

    private int nextFreePort() {
        int p = DEFAULT_PORT;
        while (repo.existsByPort(p)) p++;
        return p;
    }

    private static int checkMemory(int mb) {
        if (mb < 512 || mb > 262144) throw new BusinessException("記憶體需介於 512～262144 MB");
        return mb;
    }

    private String cleanJava(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        String p = raw.trim();
        javaService.resolve(p, 0);                       // 無法執行會直接丟出錯誤
        return p;
    }

    private static String cleanJvmArgs(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        String a = raw.trim();
        if (a.length() > 1000) throw new BusinessException("JVM 參數最多 1000 個字");
        return a;
    }

    private String uniqueDirName(String name) {
        String slug = name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (slug.length() > 24) slug = slug.substring(0, 24).replaceAll("-$", "");
        if (slug.isEmpty()) slug = "server";
        for (int i = 0; i < 20; i++) {
            byte[] b = new byte[3];
            random.nextBytes(b);
            String dir = slug + "-" + HexFormat.of().formatHex(b);
            if (!Files.exists(paths.servers().resolve(dir))) return dir;
        }
        throw new BusinessException("無法產生資料夾名稱，請重試");
    }
}
