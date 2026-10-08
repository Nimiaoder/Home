package com.liu.dev.gameserver.minecraft.version.provider;

import com.liu.dev.gameserver.minecraft.version.InstallContext;
import com.liu.dev.gameserver.minecraft.version.LaunchSpec;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/** 「下載單一 jar，複製成 server.jar 就能跑」的核心共用邏輯（Vanilla / Paper / Fabric）。 */
public abstract class AbstractJarProvider implements ServerProvider {

    public static final String SERVER_JAR = "server.jar";

    @Override
    public void install(InstallContext ctx) throws Exception {
        Files.createDirectories(ctx.serverDir());
        Files.copy(ctx.artifact(), ctx.serverDir().resolve(SERVER_JAR), StandardCopyOption.REPLACE_EXISTING);
        ctx.log().accept("已安裝 " + SERVER_JAR);
    }

    @Override
    public LaunchSpec launchSpec(Path serverDir) {
        return new LaunchSpec(List.of("-jar", SERVER_JAR, "nogui"));
    }
}
