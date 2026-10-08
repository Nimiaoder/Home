package com.liu.dev.gameserver.minecraft.server;

import com.liu.dev.gameserver.minecraft.runtime.RuntimeStatus;
import com.liu.dev.gameserver.minecraft.version.ServerType;

/** 伺服器相關 API 的輸入 / 輸出物件。 */
public final class McServerDto {

    private McServerDto() {}

    public record CreateRequest(String name, String description, ServerType type, String mcVersion, String build,
                                Integer port, Integer memoryMb, String javaPath, String jvmArgs,
                                Boolean autoStart, Boolean acceptEula) {}

    public record UpdateRequest(String name, String description, Integer port, Integer memoryMb,
                                String javaPath, String jvmArgs, Boolean autoStart) {}

    public record DeleteRequest(Boolean deleteFiles) {}

    public record ChangeVersionRequest(ServerType type, String mcVersion, String build, Boolean backupFirst) {}

    /** 伺服器完整資訊（列表與詳細頁共用）。 */
    public record View(long id, String name, String description, ServerType type, String typeName,
                       String mcVersion, String build, int port, int memoryMb, String javaPath, String jvmArgs,
                       boolean autoStart, InstallState installState, String installError, String installTaskId,
                       boolean eulaAccepted, String contentKind, String contentDir, int requiredJava,
                       String dirPath, RuntimeStatus runtime, long createdAt) {}

    public record VersionLogView(ServerType type, String mcVersion, String build, String action, long createdAt) {}
}
