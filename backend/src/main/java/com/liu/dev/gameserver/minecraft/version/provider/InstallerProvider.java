package com.liu.dev.gameserver.minecraft.version.provider;

import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.version.InstallContext;
import com.liu.dev.gameserver.minecraft.version.LaunchSpec;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** 需要「執行安裝程式」才能產生伺服器檔案的核心（Forge / NeoForge）共用邏輯。 */
public abstract class InstallerProvider implements ServerProvider {

    /** 安裝後 libraries 底下存放 unix_args.txt 的位置，例如 libraries/net/minecraftforge/forge */
    protected abstract String librariesSubPath();

    /** 舊版（1.16 以前）Forge 安裝後的 jar 檔名前綴，例如 "forge-"。 */
    protected abstract String legacyJarPrefix();

    @Override
    public boolean needsJavaToInstall() {
        return true;
    }

    @Override
    public void install(InstallContext ctx) throws Exception {
        if (ctx.javaExe() == null) throw new BusinessException("安裝 " + type().displayName() + " 需要 Java，但找不到可用的 Java");
        Files.createDirectories(ctx.serverDir());
        ProcessBuilder pb = new ProcessBuilder(ctx.javaExe(), "-jar", ctx.artifact().toAbsolutePath().toString(), "--installServer");
        pb.directory(ctx.serverDir().toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.isBlank()) ctx.log().accept(line.trim());
            }
        }
        int code = p.waitFor();
        if (code != 0) {
            throw new BusinessException(type().displayName() + " 安裝程式執行失敗（exit " + code + "），請確認網路可連到 Maven 並查看下方訊息");
        }
        // 確認真的產生了可啟動的檔案
        launchSpec(ctx.serverDir());
    }

    @Override
    public LaunchSpec launchSpec(Path serverDir) {
        Path libs = serverDir.resolve(librariesSubPath());
        if (Files.isDirectory(libs)) {
            try (Stream<Path> versions = Files.list(libs)) {
                List<Path> found = new ArrayList<>(versions.filter(v -> Files.isRegularFile(v.resolve("unix_args.txt"))).toList());
                found.sort((a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()));
                if (!found.isEmpty()) {
                    String rel = librariesSubPath() + "/" + found.get(0).getFileName() + "/unix_args.txt";
                    return new LaunchSpec(List.of("@" + rel, "nogui"));
                }
            } catch (IOException ignored) {
                // 往下找舊版 jar
            }
        }
        try (Stream<Path> files = Files.list(serverDir)) {
            List<String> jars = files.map(f -> f.getFileName().toString())
                    .filter(n -> n.startsWith(legacyJarPrefix()) && n.endsWith(".jar") && !n.contains("installer"))
                    .sorted().toList();
            if (!jars.isEmpty()) return new LaunchSpec(List.of("-jar", jars.get(0), "nogui"));
        } catch (IOException ignored) {
            // fallthrough
        }
        throw new BusinessException("找不到 " + type().displayName() + " 的啟動檔，請重新安裝版本");
    }
}
