package com.liu.dev.gameserver.minecraft.version.provider;

import com.liu.dev.gameserver.minecraft.version.*;

import java.nio.file.Path;
import java.util.List;

/**
 * 一種伺服器核心的「取得版本 → 解析下載 → 安裝 → 如何啟動」全部流程。
 * 新增核心類型（例如 Purpur、Quilt）：實作這個介面並標上 @Component，再到 ServerType 加列舉值。
 */
public interface ServerProvider {

    ServerType type();

    /** 可選的遊戲版本，新的在前。 */
    List<VersionOption> versions();

    /** 指定遊戲版本底下的建置，新的在前。 */
    List<BuildOption> builds(String mcVersion);

    /** 解析出實際下載位置。build 必須是 builds() 回傳的 id。 */
    Artifact resolve(String mcVersion, String build);

    /** 把版本庫中的檔案安裝到伺服器資料夾。 */
    void install(InstallContext ctx) throws Exception;

    /** 依伺服器資料夾內容決定啟動方式。 */
    LaunchSpec launchSpec(Path serverDir);

    /** 安裝過程是否需要執行 java（Forge / NeoForge 要跑安裝程式）。 */
    default boolean needsJavaToInstall() {
        return false;
    }

    /** 未指定建置時的預設選擇：recommended → stable → 第一個。 */
    default String defaultBuild(String mcVersion) {
        List<BuildOption> list = builds(mcVersion);
        if (list.isEmpty()) {
            throw new com.liu.dev.common.BusinessException(type().displayName() + " 沒有 " + mcVersion + " 的可用建置");
        }
        for (String ch : new String[]{"recommended", "stable"}) {
            for (BuildOption b : list) if (ch.equals(b.channel())) return b.id();
        }
        return list.get(0).id();
    }
}
