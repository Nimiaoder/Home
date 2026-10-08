package com.liu.dev.gameserver.support.task;

import com.liu.dev.common.BusinessException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 通用背景工作：下載、安裝、解壓縮、備份……任何耗時作業都丟進來，前端用 taskId 輪詢進度。
 * 任何遊戲模組都能共用。
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final long KEEP_MILLIS = 60 * 60 * 1000L;

    /** 要執行的工作。 */
    @FunctionalInterface
    public interface Job {
        void run(Context ctx) throws Exception;
    }

    /** 工作內部回報進度用。percent 傳 -1 代表不確定進度。 */
    public interface Context {
        void progress(int percent, String message);

        void message(String message);
    }

    private static final class Task implements Context {
        final String id = UUID.randomUUID().toString();
        final String title;
        final long startedAt = System.currentTimeMillis();
        volatile String status = "RUNNING";
        volatile int percent = -1;
        volatile String message = "";
        volatile String error;
        volatile long finishedAt;

        Task(String title) {
            this.title = title;
        }

        @Override
        public void progress(int p, String m) {
            this.percent = p < 0 ? -1 : Math.min(100, p);
            if (m != null) this.message = m;
        }

        @Override
        public void message(String m) {
            if (m != null) this.message = m;
        }

        TaskView view() {
            return new TaskView(id, title, status, percent, message, error, startedAt, finishedAt);
        }
    }

    /** 回給前端的任務狀態。status：RUNNING / SUCCESS / FAILED */
    public record TaskView(String id, String title, String status, int percent, String message,
                           String error, long startedAt, long finishedAt) {}

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "gs-task");
        t.setDaemon(true);
        return t;
    });

    /** 提交工作，回傳 taskId。 */
    public String submit(String title, Job job) {
        purgeOld();
        Task t = new Task(title);
        tasks.put(t.id, t);
        executor.execute(() -> {
            try {
                job.run(t);
                t.percent = 100;
                t.status = "SUCCESS";
            } catch (BusinessException e) {
                t.error = e.getMessage();
                t.status = "FAILED";
            } catch (Throwable e) {
                log.error("背景工作失敗：{}", title, e);
                t.error = "發生未預期的錯誤：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                t.status = "FAILED";
            } finally {
                t.finishedAt = System.currentTimeMillis();
            }
        });
        return t.id;
    }

    public Optional<TaskView> find(String id) {
        Task t = tasks.get(id);
        return t == null ? Optional.empty() : Optional.of(t.view());
    }

    private void purgeOld() {
        long now = System.currentTimeMillis();
        tasks.values().removeIf(t -> t.finishedAt > 0 && now - t.finishedAt > KEEP_MILLIS);
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }
}
