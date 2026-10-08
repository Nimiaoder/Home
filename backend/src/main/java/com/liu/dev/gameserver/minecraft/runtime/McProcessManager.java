package com.liu.dev.gameserver.minecraft.runtime;

import com.liu.dev.common.BusinessException;
import com.liu.dev.config.AppProperties;
import com.liu.dev.gameserver.minecraft.server.McServer;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 所有 Minecraft 行程的生命週期：啟動、停止（優雅關閉 → 逾時強制）、重啟、強制結束、送指令。
 * 後端關閉時會先讓所有伺服器存檔並正常停止。
 */
@Service
public class McProcessManager {

    private static final Logger log = LoggerFactory.getLogger(McProcessManager.class);
    private static final Pattern ANSI = Pattern.compile("\u001B\\[[0-9;?]*[ -/]*[@-~]");
    private static final Pattern DONE = Pattern.compile("Done \\(\\d+(?:[.,]\\d+)?s\\)!");
    private static final Pattern JOIN = Pattern.compile("(?:\\]:|:) (\\S+) joined the game\\s*$");
    private static final Pattern LEAVE = Pattern.compile("(?:\\]:|:) (\\S+) left the game\\s*$");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<Long, McProcess> processes = new ConcurrentHashMap<>();
    private final ConsoleHub hub;
    private final LaunchCommandBuilder launcher;
    private final int stopTimeoutSeconds;

    private final ScheduledExecutorService watchdog = Executors.newSingleThreadScheduledExecutor(daemon("mc-watchdog"));
    private final ExecutorService lifecycle = Executors.newCachedThreadPool(daemon("mc-lifecycle"));

    public McProcessManager(ConsoleHub hub, LaunchCommandBuilder launcher, AppProperties props) {
        this.hub = hub;
        this.launcher = launcher;
        Integer t = props.minecraft() == null ? null : props.minecraft().stopTimeoutSeconds();
        this.stopTimeoutSeconds = t == null || t < 5 ? 60 : t;
    }

    private static ThreadFactory daemon(String name) {
        return r -> {
            Thread t = new Thread(r, name);
            t.setDaemon(true);
            return t;
        };
    }

    // ---------------------------------------------------------------- 查詢

    public RuntimeStatus status(long serverId) {
        McProcess p = processes.get(serverId);
        return p == null ? RuntimeStatus.stopped(serverId) : p.status();
    }

    /** 行程是否還活著（STARTING / RUNNING / STOPPING）。 */
    public boolean isRunning(long serverId) {
        McProcess p = processes.get(serverId);
        return p != null && p.isAlive();
    }

    // ---------------------------------------------------------------- 操作

    public synchronized RuntimeStatus start(McServer s) {
        McProcess old = processes.get(s.getId());
        if (old != null && old.isAlive()) throw new BusinessException("伺服器已經在執行中");

        LaunchCommandBuilder.Plan plan = launcher.build(s);
        ConsoleChannel ch = hub.channelFor(s);
        ch.append("──────── 啟動伺服器 " + LocalDateTime.now().format(TIME) + " ────────");
        ch.append("$ " + String.join(" ", plan.command()));

        Process process;
        try {
            process = new ProcessBuilder(plan.command())
                    .directory(plan.workDir().toFile())
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException e) {
            throw new BusinessException("無法啟動 Java 行程：" + e.getMessage());
        }
        McProcess mp = new McProcess(s.getId(), process);
        processes.put(s.getId(), mp);
        Thread reader = new Thread(() -> pump(mp, ch), "mc-console-" + s.getId());
        reader.setDaemon(true);
        reader.start();
        ch.publishStatus(mp.status());
        log.info("Minecraft 伺服器 {} 已啟動（pid {}）", s.getName(), process.pid());
        return mp.status();
    }

    /** 優雅停止：送出 stop，逾時後送 SIGTERM，再逾時才強制結束。 */
    public RuntimeStatus stop(McServer s) {
        McProcess mp = processes.get(s.getId());
        if (mp == null || !mp.isAlive()) throw new BusinessException("伺服器目前沒有在執行");
        ConsoleChannel ch = hub.channelFor(s);
        if (mp.state() == ServerState.STOPPING) return mp.status();
        log.info("正在停止伺服器「{}」（最多等 {} 秒）", s.getName(), stopTimeoutSeconds);
        beginStop(mp, ch);
        return mp.status();
    }

    private void beginStop(McProcess mp, ConsoleChannel ch) {
        mp.requestStop();
        mp.setState(ServerState.STOPPING);
        ch.append("[Dev Console] 正在停止伺服器…");
        ch.publishStatus(mp.status());
        mp.sendLine("stop");
        watchdog.schedule(() -> {
            if (!mp.isAlive()) return;
            ch.append("[Dev Console] 超過 " + stopTimeoutSeconds + " 秒仍未關閉，送出終止訊號");
            mp.process().destroy();
            watchdog.schedule(() -> {
                if (mp.isAlive()) {
                    ch.append("[Dev Console] 仍未結束，強制終止");
                    mp.process().destroyForcibly();
                }
            }, 15, TimeUnit.SECONDS);
        }, stopTimeoutSeconds, TimeUnit.SECONDS);
    }

    /** 非同步重啟：先停止，等行程結束後再啟動。 */
    public void restart(McServer s) {
        log.info("重新啟動伺服器「{}」", s.getName());
        ConsoleChannel ch = hub.channelFor(s);
        McProcess mp = processes.get(s.getId());
        if (mp == null || !mp.isAlive()) {
            start(s);
            return;
        }
        if (mp.state() != ServerState.STOPPING) beginStop(mp, ch);
        lifecycle.execute(() -> {
            try {
                if (!mp.awaitFinished((stopTimeoutSeconds + 30L) * 1000)) {
                    ch.append("[Dev Console] 重新啟動失敗：舊的行程沒有結束");
                    return;
                }
                start(s);
            } catch (BusinessException e) {
                ch.append("[Dev Console] 重新啟動失敗：" + e.getMessage());
                ch.publishStatus(status(s.getId()));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void kill(McServer s) {
        McProcess mp = processes.get(s.getId());
        if (mp == null || !mp.isAlive()) throw new BusinessException("伺服器目前沒有在執行");
        mp.requestStop();
        log.warn("強制終止伺服器「{}」（pid {}）", s.getName(), mp.process().pid());
        hub.channelFor(s).append("[Dev Console] 強制終止伺服器");
        mp.process().destroyForcibly();
    }

    /** 在主控台輸入一行指令。 */
    public void sendCommand(McServer s, String raw) {
        String cmd = raw == null ? "" : raw.replaceAll("[\\r\\n]+", " ").trim();
        if (cmd.isEmpty()) throw new BusinessException("請輸入指令");
        if (cmd.length() > 1000) throw new BusinessException("指令太長");
        McProcess mp = processes.get(s.getId());
        if (mp == null || !mp.isAlive()) throw new BusinessException("伺服器目前沒有在執行，無法送出指令");
        ConsoleChannel ch = hub.channelFor(s);
        if (cmd.equalsIgnoreCase("stop") || cmd.equalsIgnoreCase("/stop")) {
            stop(s);
            return;
        }
        log.debug("伺服器「{}」收到指令：{}", s.getName(), cmd);
        ch.append("> " + cmd);
        if (!mp.sendLine(cmd)) throw new BusinessException("無法寫入伺服器（行程可能正在結束）");
    }

    /** 直接送指令，不在主控台回顯（供備份時送 save-off 等內部指令使用）。 */
    public boolean sendInternal(McServer s, String cmd) {
        McProcess mp = processes.get(s.getId());
        if (mp == null || !mp.isAlive()) return false;
        hub.channelFor(s).append("[Dev Console] > " + cmd);
        return mp.sendLine(cmd);
    }

    // ---------------------------------------------------------------- 輸出處理

    private void pump(McProcess mp, ConsoleChannel ch) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(mp.process().getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) handleLine(mp, ch, ANSI.matcher(line).replaceAll(""));
        } catch (IOException ignored) {
            // 行程結束時串流會關閉
        }
        int code;
        try {
            code = mp.process().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            code = -1;
        }
        mp.markFinished(code);
        ch.append("[Dev Console] 伺服器已停止（exit code " + code + "）" + (mp.state() == ServerState.CRASHED ? "——非預期結束" : ""));
        ch.publishStatus(mp.status());
    }

    private void handleLine(McProcess mp, ConsoleChannel ch, String line) {
        ch.append(line);
        boolean changed = false;
        if (mp.state() == ServerState.STARTING && DONE.matcher(line).find()) {
            mp.setState(ServerState.RUNNING);
            changed = true;
        }
        Matcher j = JOIN.matcher(line);
        if (j.find()) changed |= mp.players().add(j.group(1));
        Matcher l = LEAVE.matcher(line);
        if (l.find()) changed |= mp.players().remove(l.group(1));
        if (changed) ch.publishStatus(mp.status());
    }

    // ---------------------------------------------------------------- 關閉

    @PreDestroy
    void shutdownAll() {
        long deadline = System.currentTimeMillis() + stopTimeoutSeconds * 1000L;
        for (McProcess mp : processes.values()) {
            if (mp.isAlive()) {
                mp.requestStop();
                mp.sendLine("stop");
            }
        }
        for (McProcess mp : processes.values()) {
            try {
                long left = Math.max(1, deadline - System.currentTimeMillis());
                if (mp.isAlive() && !mp.awaitFinished(left)) mp.process().destroyForcibly();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                mp.process().destroyForcibly();
            }
        }
        watchdog.shutdownNow();
        lifecycle.shutdownNow();
    }
}
