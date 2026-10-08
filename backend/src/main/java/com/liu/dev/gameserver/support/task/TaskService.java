package com.liu.dev.gameserver.support.task;

import com.liu.dev.common.BusinessException;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 通用背景工作：下載、安裝、解壓縮、備份……任何耗時作業都丟進來，前端用 taskId 輪詢進度。
 * 每個工作可帶一個 scope（例如 mc:java），前端重新整理後用 scope 找回進行中的工作。
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

        /** 回報目前檔案的下載量（已下載, 總大小或 -1），後端據此算出下載速率。 */
        default void transfer(long done, long total) {
        }

        /** 檔案下載結束，清除下載量與速率。 */
        default void clearTransfer() {
        }
    }

    private static final class Task implements Context {
        final String id = UUID.randomUUID().toString();
        final String scope;
        final String title;
        final long startedAt = System.currentTimeMillis();
        volatile String status = "RUNNING";
        volatile int percent = -1;
        volatile String message = "";
        volatile String error;
        volatile long finishedAt;
        volatile long bytesDone;
        volatile long bytesTotal = -1;
        volatile long speed;                       // bytes / 秒
        private long sampleAt;
        private long sampleBytes;

        Task(String scope, String title) {
            this.scope = scope == null ? "" : scope;
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

        @Override
        public void transfer(long done, long total) {
            long now = System.currentTimeMillis();
            if (sampleAt == 0 || done < sampleBytes) {            // 第一次回報，或開始下載另一個檔案
                sampleAt = now;
                sampleBytes = done;
                speed = 0;
            } else if (now - sampleAt >= 1000) {                  // 每秒取樣一次，再與前一次平滑
                long inst = (done - sampleBytes) * 1000 / (now - sampleAt);
                speed = speed == 0 ? inst : (long) (speed * 0.6 + inst * 0.4);
                sampleAt = now;
                sampleBytes = done;
            }
            bytesDone = done;
            bytesTotal = total;
        }

        @Override
        public void clearTransfer() {
            bytesDone = 0;
            bytesTotal = -1;
            speed = 0;
            sampleAt = 0;
            sampleBytes = 0;
        }

        TaskView view() {
            return new TaskView(id, scope, title, status, percent, message, error, startedAt, finishedAt,
                    bytesDone, bytesTotal, speed);
        }
    }

    /** 回給前端的任務狀態。status：RUNNING / SUCCESS / FAILED；bytesTotal 為 -1 表示不知道總大小。 */
    public record TaskView(String id, String scope, String title, String status, int percent, String message,
                           String error, long startedAt, long finishedAt,
                           long bytesDone, long bytesTotal, long speed) {}

    private final Map<String, Task> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "gs-task");
        t.setDaemon(true);
        return t;
    });

    /** 提交工作（不分類），回傳 taskId。 */
    public String submit(String title, Job job) {
        return submit("", title, job);
    }

    /**
     * 提交工作，回傳 taskId。
     *
     * @param scope 工作分類，前端重新整理後靠它找回進行中的工作，例如 mc:java、mc:mods:3
     */
    public String submit(String scope, String title, Job job) {
        purgeOld();
        Task t = new Task(scope, title);
        tasks.put(t.id, t);
        log.info("背景工作開始：{}（scope={}，id={}）", title, t.scope, t.id);
        executor.execute(() -> {
            try {
                job.run(t);
                t.percent = 100;
                t.status = "SUCCESS";
                log.info("背景工作完成：{}（耗時 {} ms）", title, System.currentTimeMillis() - t.startedAt);
            } catch (BusinessException e) {
                t.error = e.getMessage();
                t.status = "FAILED";
                log.warn("背景工作失敗：{}，原因：{}", title, e.getMessage());
            } catch (Throwable e) {
                log.error("背景工作發生未預期的錯誤：{}", title, e);
                t.error = "發生未預期的錯誤：" + (e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage());
                t.status = "FAILED";
            } finally {
                t.clearTransfer();
                t.finishedAt = System.currentTimeMillis();
            }
        });
        return t.id;
    }

    public Optional<TaskView> find(String id) {
        Task t = tasks.get(id);
        return t == null ? Optional.empty() : Optional.of(t.view());
    }

    /** 進行中的工作，新的在前；scope 為空則回傳全部。 */
    public List<TaskView> running(String scope) {
        return tasks.values().stream()
                .filter(t -> "RUNNING".equals(t.status))
                .filter(t -> scope == null || scope.isBlank() || scope.equals(t.scope))
                .sorted(Comparator.comparingLong((Task t) -> t.startedAt).reversed())
                .map(Task::view)
                .toList();
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
