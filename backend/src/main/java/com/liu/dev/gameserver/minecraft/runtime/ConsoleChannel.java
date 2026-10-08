package com.liu.dev.gameserver.minecraft.runtime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * 單一伺服器的主控台：環形緩衝 + SSE 訂閱者。
 *
 * <p>重點設計：讀取 Minecraft 輸出的執行緒只負責 append（不會被慢速瀏覽器卡住）；
 * 實際寫給瀏覽器的動作都在獨立執行緒（pushExecutor）完成。否則瀏覽器斷線卡住時，
 * Minecraft 的 stdout 管線會塞滿，整個伺服器就會凍結。</p>
 */
public final class ConsoleChannel {

    private static final int BATCH = 200;

    private final int capacity;
    private final Executor pushExecutor;
    private final ObjectMapper mapper;
    private final long epoch = System.currentTimeMillis();

    private final ArrayDeque<ConsoleLine> buffer = new ArrayDeque<>();
    private long seq = 0;
    private final CopyOnWriteArrayList<Subscriber> subscribers = new CopyOnWriteArrayList<>();

    ConsoleChannel(int capacity, Executor pushExecutor, ObjectMapper mapper) {
        this.capacity = Math.max(100, capacity);
        this.pushExecutor = pushExecutor;
        this.mapper = mapper;
    }

    // ---------------------------------------------------------------- 緩衝

    public synchronized ConsoleLine append(String text) {
        ConsoleLine line = new ConsoleLine(++seq, text);
        buffer.addLast(line);
        while (buffer.size() > capacity) buffer.removeFirst();
        for (Subscriber s : subscribers) s.wake();
        return line;
    }

    public synchronized long lastSeq() {
        return seq;
    }

    synchronized List<ConsoleLine> after(long afterSeq, int limit) {
        List<ConsoleLine> out = new ArrayList<>();
        for (ConsoleLine l : buffer) {
            if (l.seq() > afterSeq) {
                out.add(l);
                if (out.size() >= limit) break;
            }
        }
        return out;
    }

    /** 等待某一行符合條件（最多 timeoutMillis）。用於「存檔完成」這類需要等回應的情境。 */
    public boolean awaitMatch(long afterSeq, Predicate<String> matcher, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        long cursor = afterSeq;
        while (System.currentTimeMillis() < deadline) {
            for (ConsoleLine l : after(cursor, 500)) {
                cursor = l.seq();
                if (matcher.test(l.text())) return true;
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- 狀態推送

    public void publishStatus(RuntimeStatus status) {
        String json = toJson(status);
        for (Subscriber s : subscribers) s.pushStatus(json);
    }

    void heartbeat() {
        for (Subscriber s : subscribers) s.ping();
    }

    private String toJson(Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    // ---------------------------------------------------------------- 訂閱

    /**
     * 建立 SSE 訂閱。clientEpoch 與目前 epoch 不同（例如後端重啟過）時，從頭重送並通知前端清除舊內容。
     */
    public SseEmitter subscribe(long after, long clientEpoch, RuntimeStatus current) {
        boolean reset = clientEpoch != epoch;
        Subscriber sub = new Subscriber(reset ? 0 : after, reset);
        sub.pendingStatus.set(toJson(current));
        subscribers.add(sub);
        sub.wake();
        return sub.emitter;
    }

    private final class Subscriber {
        final SseEmitter emitter = new SseEmitter(0L);          // 0 = 永不逾時
        final AtomicBoolean scheduled = new AtomicBoolean();
        final AtomicReference<String> pendingStatus = new AtomicReference<>();
        final boolean reset;
        volatile long lastSent;
        volatile boolean needHello = true;
        volatile boolean pingRequested;
        volatile boolean closed;

        Subscriber(long lastSent, boolean reset) {
            this.lastSent = lastSent;
            this.reset = reset;
            emitter.onCompletion(this::close);
            emitter.onTimeout(this::close);
            emitter.onError(e -> close());
        }

        void pushStatus(String json) {
            pendingStatus.set(json);
            wake();
        }

        void ping() {
            pingRequested = true;
            wake();
        }

        void wake() {
            if (closed) return;
            if (scheduled.compareAndSet(false, true)) {
                try {
                    pushExecutor.execute(this::flush);
                } catch (RuntimeException e) {
                    scheduled.set(false);
                    close();
                }
            }
        }

        boolean hasWork() {
            return needHello || pingRequested || pendingStatus.get() != null || lastSeq() > lastSent;
        }

        void flush() {
            try {
                while (!closed) {
                    boolean did = false;
                    if (needHello) {
                        needHello = false;
                        emitter.send(SseEmitter.event().name("hello")
                                .data("{\"epoch\":" + epoch + ",\"reset\":" + reset + "}"));
                        did = true;
                    }
                    List<ConsoleLine> batch = after(lastSent, BATCH);
                    if (!batch.isEmpty()) {
                        emitter.send(SseEmitter.event().name("lines").data(toJson(batch)));
                        lastSent = batch.get(batch.size() - 1).seq();
                        did = true;
                    }
                    String st = pendingStatus.getAndSet(null);
                    if (st != null) {
                        emitter.send(SseEmitter.event().name("status").data(st));
                        did = true;
                    }
                    if (pingRequested) {
                        pingRequested = false;
                        emitter.send(SseEmitter.event().comment("ping"));
                        did = true;
                    }
                    if (!did) break;
                }
            } catch (Exception e) {
                close();                                        // 瀏覽器已離線
            } finally {
                scheduled.set(false);
                if (!closed && hasWork()) wake();               // 避免漏掉剛好在收尾時進來的資料
            }
        }

        void close() {
            if (closed) return;
            closed = true;
            subscribers.remove(this);
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // 已結束
            }
        }
    }
}
