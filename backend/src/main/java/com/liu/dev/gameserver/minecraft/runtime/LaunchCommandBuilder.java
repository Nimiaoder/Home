package com.liu.dev.gameserver.minecraft.runtime;

import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.java.JavaRequirement;
import com.liu.dev.gameserver.minecraft.java.JavaRuntimeService;
import com.liu.dev.gameserver.minecraft.server.InstallState;
import com.liu.dev.gameserver.minecraft.server.McServer;
import com.liu.dev.gameserver.minecraft.settings.EulaFile;
import com.liu.dev.gameserver.minecraft.settings.ServerPropertiesService;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.minecraft.version.LaunchSpec;
import com.liu.dev.gameserver.minecraft.version.provider.ProviderRegistry;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** 啟動前檢查（安裝狀態、EULA、Java、連接埠）並組出完整的 java 指令。 */
@Component
public class LaunchCommandBuilder {

    /** 啟動計畫。 */
    public record Plan(List<String> command, Path workDir, String javaPath, int javaMajor) {}

    private final MinecraftPaths paths;
    private final ProviderRegistry providers;
    private final JavaRuntimeService javaService;
    private final ServerPropertiesService properties;

    public LaunchCommandBuilder(MinecraftPaths paths, ProviderRegistry providers,
                                JavaRuntimeService javaService, ServerPropertiesService properties) {
        this.paths = paths;
        this.providers = providers;
        this.javaService = javaService;
        this.properties = properties;
    }

    public Plan build(McServer s) {
        if (s.getInstallState() != InstallState.READY) {
            throw new BusinessException("伺服器檔案尚未安裝完成（目前狀態：" + s.getInstallState() + "）");
        }
        Path dir = paths.serverDir(s.getDirName());
        if (!Files.isDirectory(dir)) throw new BusinessException("找不到伺服器資料夾：" + dir);
        if (!EulaFile.isAccepted(dir)) throw new BusinessException("尚未同意 Minecraft EULA，請先到「總覽」同意後再啟動");

        LaunchSpec spec = providers.get(s.getType()).launchSpec(dir);

        int required = JavaRequirement.forMinecraft(s.getMcVersion());
        JavaRuntimeService.JavaChoice java = javaService.resolve(s.getJavaPath(), required);
        if (java.major() < required) {
            throw new BusinessException("Minecraft " + s.getMcVersion() + " 需要 Java " + required
                    + " 以上，但目前使用的是 Java " + java.major() + "（" + java.path() + "）");
        }

        checkPortFree(s.getPort());
        properties.set(s.getDirName(), "server-port", String.valueOf(s.getPort()));

        int xmx = s.getMemoryMb();
        int xms = Math.min(512, xmx);
        List<String> cmd = new ArrayList<>();
        cmd.add(java.path());
        cmd.add("-Xms" + xms + "M");
        cmd.add("-Xmx" + xmx + "M");
        // 讓主控台的中文（玩家名稱、聊天）不亂碼
        cmd.add("-Dfile.encoding=UTF-8");
        cmd.add("-Dstdout.encoding=UTF-8");
        cmd.add("-Dstderr.encoding=UTF-8");
        if (StringUtils.hasText(s.getJvmArgs())) cmd.addAll(splitArgs(s.getJvmArgs()));
        cmd.addAll(spec.args());
        return new Plan(cmd, dir, java.path(), java.major());
    }

    private static void checkPortFree(int port) {
        try (ServerSocket ss = new ServerSocket()) {
            ss.setReuseAddress(true);
            ss.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            throw new BusinessException("連接埠 " + port + " 已被占用（可能有其他伺服器或程式正在使用），請到「設定」更換連接埠");
        }
    }

    /** 以空白分隔參數，支援 "雙引號" 與 '單引號'。 */
    static List<String> splitArgs(String s) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;
        boolean has = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quote != 0) {
                if (c == quote) quote = 0;
                else cur.append(c);
            } else if (c == '"' || c == '\'') {
                quote = c;
                has = true;
            } else if (Character.isWhitespace(c)) {
                if (has || cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                    has = false;
                }
            } else {
                cur.append(c);
            }
        }
        if (has || cur.length() > 0) out.add(cur.toString());
        return out;
    }
}
