package com.liu.dev.gameserver.minecraft.settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** eula.txt 讀寫。Minecraft 伺服器必須同意 EULA（https://aka.ms/MinecraftEULA）才能啟動。 */
public final class EulaFile {

    private EulaFile() {}

    public static boolean isAccepted(Path serverDir) {
        Path f = serverDir.resolve("eula.txt");
        if (!Files.isRegularFile(f)) return false;
        try {
            for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
                if (line.trim().equalsIgnoreCase("eula=true")) return true;
            }
        } catch (IOException ignored) {
            // 讀不到視為未同意
        }
        return false;
    }

    public static void accept(Path serverDir) throws IOException {
        Files.createDirectories(serverDir);
        Files.writeString(serverDir.resolve("eula.txt"),
                "# 已透過管理介面同意 Mojang EULA (https://aka.ms/MinecraftEULA)\neula=true\n", StandardCharsets.UTF_8);
    }
}
