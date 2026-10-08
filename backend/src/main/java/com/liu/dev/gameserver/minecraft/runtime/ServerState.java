package com.liu.dev.gameserver.minecraft.runtime;

/** 伺服器執行狀態。 */
public enum ServerState {
    STOPPED,
    /** Java 已啟動，但還沒出現 "Done" 訊息。 */
    STARTING,
    RUNNING,
    /** 已送出 stop，等待關閉中。 */
    STOPPING,
    /** 非預期結束（exit code 不為 0 且不是我們要求停止的）。 */
    CRASHED
}
