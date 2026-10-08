package com.liu.dev.gameserver.minecraft.runtime;

import java.util.List;

/** 伺服器目前的執行狀態（給前端顯示，也會透過 SSE 即時推送）。 */
public record RuntimeStatus(long serverId, ServerState state, Long pid, Long startedAt, long uptimeSeconds,
                            Long memoryMb, List<String> players, Integer exitCode) {

    public static RuntimeStatus stopped(long serverId) {
        return new RuntimeStatus(serverId, ServerState.STOPPED, null, null, 0, null, List.of(), null);
    }
}
