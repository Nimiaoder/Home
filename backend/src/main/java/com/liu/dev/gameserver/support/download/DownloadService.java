package com.liu.dev.gameserver.support.download;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一次性下載票券。
 * 瀏覽器無法在連結上帶 Authorization，所以先用已登入的 API 換一張短效票券，
 * 再用 /api/dl/{ticket} 直接串流下載（大檔不必先載入記憶體）。
 */
@Service
public class DownloadService {

    private static final long TTL_MILLIS = 2 * 60 * 1000L;

    private record Ticket(DownloadSource source, long expiresAt) {}

    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public String issue(DownloadSource source) {
        long now = System.currentTimeMillis();
        tickets.values().removeIf(t -> t.expiresAt() < now);
        String id = UUID.randomUUID().toString().replace("-", "");
        tickets.put(id, new Ticket(source, now + TTL_MILLIS));
        return id;
    }

    /** 取出並作廢（一次性）。 */
    public Optional<DownloadSource> consume(String id) {
        Ticket t = tickets.remove(id);
        if (t == null || t.expiresAt() < System.currentTimeMillis()) return Optional.empty();
        return Optional.of(t.source());
    }
}
