package com.liu.dev.gameserver.minecraft.runtime;

import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.server.InstallState;
import com.liu.dev.gameserver.minecraft.server.McServer;
import com.liu.dev.gameserver.minecraft.server.McServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** 後端啟動時，自動啟動標記為「開機自動啟動」的伺服器（逐一啟動，避免同時吃滿 CPU 與記憶體）。 */
@Component
public class McStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(McStartupRunner.class);

    private final McServerRepository repo;
    private final McProcessManager processes;

    public McStartupRunner(McServerRepository repo, McProcessManager processes) {
        this.repo = repo;
        this.processes = processes;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread t = new Thread(() -> {
            try {
                for (McServer s : repo.findByAutoStartTrue()) {
                    if (s.getInstallState() != InstallState.READY) continue;
                    try {
                        processes.start(s);
                        Thread.sleep(2000);
                    } catch (BusinessException e) {
                        log.warn("自動啟動 {} 失敗：{}", s.getName(), e.getMessage());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("自動啟動伺服器時發生錯誤", e);
            }
        }, "mc-autostart");
        t.setDaemon(true);
        t.start();
    }
}
