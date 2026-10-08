package com.liu.dev.gameserver.minecraft.server;

/** 伺服器檔案的安裝狀態。 */
public enum InstallState {
    /** 下載或安裝中。 */
    INSTALLING,
    /** 可啟動。 */
    READY,
    /** 安裝失敗，需要重試或切換版本。 */
    FAILED
}
