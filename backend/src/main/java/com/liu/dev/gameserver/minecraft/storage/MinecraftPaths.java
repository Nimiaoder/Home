package com.liu.dev.gameserver.minecraft.storage;

import com.liu.dev.config.AppProperties;
import com.liu.dev.gameserver.support.path.SafePaths;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Minecraft 所有檔案的目錄配置（唯一的路徑出入口，其他類別不要自己拼路徑）。
 *
 * <pre>
 * {base}/
 *   servers/{dirName}/        每個伺服器自己的工作目錄（server.properties、world/、mods/、logs/ ...）
 *   library/{TYPE}/{mc}/{build}/   已下載的伺服器核心（版本庫，各伺服器共用）
 *   backups/{dirName}/        地圖備份（zip）
 *   java/                     由本系統下載的 Java
 *   tmp/                      上傳與下載的暫存檔
 * </pre>
 */
@Component
public class MinecraftPaths {

    public static final String DEFAULT_BASE = "/volume1/LiuChenWei/Home/GameServer/Minecraft";
    private static final Logger log = LoggerFactory.getLogger(MinecraftPaths.class);

    private final Path base;

    public MinecraftPaths(AppProperties props) {
        String dir = props.minecraft() == null ? null : props.minecraft().baseDir();
        this.base = Paths.get(StringUtils.hasText(dir) ? dir : DEFAULT_BASE).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            for (Path p : List.of(servers(), library(), backups(), java(), tmp())) Files.createDirectories(p);
            log.info("Minecraft 資料目錄：{}", base);
        } catch (IOException e) {
            // 開發機沒有 /volume1 時不要讓整個系統起不來；實際操作時會回報明確錯誤
            log.warn("無法建立 Minecraft 資料目錄 {}：{}（請確認路徑存在且有寫入權限，或設定 MC_BASE_DIR）", base, e.getMessage());
        }
    }

    public Path base() {
        return base;
    }

    public Path servers() {
        return base.resolve("servers");
    }

    public Path library() {
        return base.resolve("library");
    }

    public Path backups() {
        return base.resolve("backups");
    }

    public Path java() {
        return base.resolve("java");
    }

    public Path tmp() {
        return base.resolve("tmp");
    }

    public Path serverDir(String dirName) {
        return SafePaths.resolveInside(servers(), dirName);
    }

    public Path backupDir(String dirName) {
        return SafePaths.resolveInside(backups(), dirName);
    }

    public Path libraryDir(String type, String mcVersion, String build) {
        return SafePaths.resolveInside(library(),
                SafePaths.requireName(type, "類型"),
                SafePaths.requireName(mcVersion, "遊戲版本"),
                SafePaths.requireName(build, "建置版本"));
    }
}
