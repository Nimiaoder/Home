package com.liu.dev.gameserver.minecraft.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liu.dev.config.AppProperties;
import com.liu.dev.gameserver.minecraft.server.McServer;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.support.io.FileTool;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 管理所有伺服器的 ConsoleChannel。主控台內容獨立於行程：伺服器停止、重啟後仍保留歷史。
 * 第一次建立時會用 logs/latest.log 的尾端內容當作初始畫面，所以沒啟動過也看得到上次的紀錄。
 */
@Component
public class ConsoleHub {

    private static final int SEED_LINES = 200;

    private final Map<Long, ConsoleChannel> channels = new ConcurrentHashMap<>();
    private final ExecutorService pushExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "mc-console-heartbeat");
        t.setDaemon(true);
        return t;
    });
    private final ObjectMapper mapper;
    private final MinecraftPaths paths;
    private final int capacity;

    public ConsoleHub(ObjectMapper mapper, MinecraftPaths paths, AppProperties props) {
        this.mapper = mapper;
        this.paths = paths;
        Integer cap = props.minecraft() == null ? null : props.minecraft().consoleBufferLines();
        this.capacity = cap == null ? 2000 : cap;
    }

    @PostConstruct
    void start() {
        scheduler.scheduleWithFixedDelay(() -> channels.values().forEach(ConsoleChannel::heartbeat),
                20, 20, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stop() {
        scheduler.shutdownNow();
        pushExecutor.shutdownNow();
    }

    public ConsoleChannel channelFor(McServer s) {
        return channels.computeIfAbsent(s.getId(), id -> {
            ConsoleChannel ch = new ConsoleChannel(capacity, pushExecutor, mapper);
            for (String line : FileTool.tailLines(paths.serverDir(s.getDirName()).resolve("logs").resolve("latest.log"), SEED_LINES)) {
                ch.append(line);
            }
            return ch;
        });
    }

    public Optional<ConsoleChannel> existing(long serverId) {
        return Optional.ofNullable(channels.get(serverId));
    }

    public void remove(long serverId) {
        channels.remove(serverId);
    }
}
