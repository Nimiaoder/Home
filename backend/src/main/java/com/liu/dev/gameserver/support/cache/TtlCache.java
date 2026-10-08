package com.liu.dev.gameserver.support.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** 極簡的記憶體快取（含存活時間）。外部版本清單 API 不需要每次都打。 */
public class TtlCache<K, V> {

    private record Entry<V>(V value, long expiresAt) {}

    private final ConcurrentHashMap<K, Entry<V>> map = new ConcurrentHashMap<>();
    private final long ttlMillis;

    public TtlCache(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    /** 命中且未過期就回傳快取，否則呼叫 loader（loader 丟例外時不會寫入快取）。 */
    public V get(K key, Supplier<V> loader) {
        long now = System.currentTimeMillis();
        Entry<V> e = map.get(key);
        if (e != null && e.expiresAt() > now) return e.value();
        V v = loader.get();
        map.put(key, new Entry<>(v, now + ttlMillis));
        return v;
    }

    public void clear() {
        map.clear();
    }
}
