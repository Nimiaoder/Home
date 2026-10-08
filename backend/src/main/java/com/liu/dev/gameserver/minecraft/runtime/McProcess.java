package com.liu.dev.gameserver.minecraft.runtime;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** 一次「啟動」對應的 Java 行程（包含狀態、玩家、stdin）。 */
final class McProcess {

    private final long serverId;
    private final Process process;
    private final long startedAt = System.currentTimeMillis();
    private final BufferedWriter stdin;
    private final Object stdinLock = new Object();
    private final Set<String> players = ConcurrentHashMap.newKeySet();
    private final CountDownLatch finished = new CountDownLatch(1);

    private volatile ServerState state = ServerState.STARTING;
    private volatile boolean stopRequested;
    private volatile Integer exitCode;

    McProcess(long serverId, Process process) {
        this.serverId = serverId;
        this.process = process;
        this.stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8));
    }

    Process process() {
        return process;
    }

    boolean isAlive() {
        return process.isAlive();
    }

    ServerState state() {
        return state;
    }

    void setState(ServerState s) {
        this.state = s;
    }

    boolean stopRequested() {
        return stopRequested;
    }

    void requestStop() {
        this.stopRequested = true;
    }

    Set<String> players() {
        return players;
    }

    /** 送一行文字到伺服器 stdin（等同在主控台輸入指令）。 */
    boolean sendLine(String line) {
        synchronized (stdinLock) {
            try {
                stdin.write(line);
                stdin.newLine();
                stdin.flush();
                return true;
            } catch (IOException e) {
                return false;
            }
        }
    }

    /** 行程結束時由讀取執行緒呼叫。 */
    void markFinished(int code) {
        this.exitCode = code;
        this.players.clear();
        this.state = (stopRequested || code == 0) ? ServerState.STOPPED : ServerState.CRASHED;
        finished.countDown();
    }

    boolean awaitFinished(long timeoutMillis) throws InterruptedException {
        return finished.await(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    RuntimeStatus status() {
        boolean alive = isAlive() && finished.getCount() > 0;
        long uptime = alive ? (System.currentTimeMillis() - startedAt) / 1000 : 0;
        List<String> ps = new ArrayList<>(players);
        Collections.sort(ps);
        return new RuntimeStatus(serverId, state, alive ? process.pid() : null, alive ? startedAt : null,
                uptime, alive ? rssMb(process.pid()) : null, ps, exitCode);
    }

    /** Linux：讀 /proc/{pid}/status 的 VmRSS（實際使用記憶體）。其他系統回傳 null。 */
    private static Long rssMb(long pid) {
        try {
            for (String l : Files.readAllLines(Path.of("/proc/" + pid + "/status"))) {
                if (l.startsWith("VmRSS:")) {
                    return Long.parseLong(l.replaceAll("[^0-9]", "")) / 1024;
                }
            }
        } catch (Exception ignored) {
            // 非 Linux 或行程已結束
        }
        return null;
    }
}
