package com.liu.dev.gameserver.minecraft.version.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.version.*;
import com.liu.dev.gameserver.support.cache.TtlCache;
import com.liu.dev.gameserver.support.http.HttpFetcher;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * NeoForge。資料來源：maven.neoforged.net。
 * NeoForge 版本號直接編碼了 Minecraft 版本：
 * 舊制 21.1.200 → 1.21.1、21.0.5 → 1.21；2026 起 26.1.2.76 → 26.1.2、26.1.0.10-beta → 26.1。
 */
@Component
public class NeoForgeProvider extends InstallerProvider {

    private static final String LIST_API = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge";
    private static final String MAVEN = "https://maven.neoforged.net/releases/net/neoforged/neoforge";

    private final HttpFetcher http;
    private final TtlCache<String, List<String>> cache = new TtlCache<>(10 * 60 * 1000L);

    public NeoForgeProvider(HttpFetcher http) {
        this.http = http;
    }

    @Override
    public ServerType type() {
        return ServerType.NEOFORGE;
    }

    @Override
    protected String librariesSubPath() {
        return "libraries/net/neoforged/neoforge";
    }

    @Override
    protected String legacyJarPrefix() {
        return "neoforge-";
    }

    private List<String> allVersions() {
        return cache.get("all", () -> {
            JsonNode arr = http.getJson(LIST_API).path("versions");
            List<String> out = new ArrayList<>();
            for (JsonNode v : arr) out.add(v.asText());
            if (out.isEmpty()) throw new BusinessException("無法取得 NeoForge 版本清單");
            return out;
        });
    }

    /** NeoForge 版本 → Minecraft 版本；無法辨識回傳 null。 */
    static String toMinecraft(String neo) {
        String core = neo.replaceAll("-.*$", "");
        String[] p = core.split("\\.");
        for (String s : p) if (!s.matches("\\d+")) return null;
        if (p.length < 3) return null;
        int major = Integer.parseInt(p[0]);
        if (major >= 26) {
            if (p.length < 4) return null;
            return "0".equals(p[2]) ? p[0] + "." + p[1] : p[0] + "." + p[1] + "." + p[2];
        }
        return "0".equals(p[1]) ? "1." + p[0] : "1." + p[0] + "." + p[1];
    }

    @Override
    public List<VersionOption> versions() {
        Set<String> mcs = new LinkedHashSet<>();
        for (String v : allVersions()) {
            String mc = toMinecraft(v);
            if (mc != null) mcs.add(mc);
        }
        List<String> sorted = new ArrayList<>(mcs);
        sorted.sort(McVersions.NEWEST_FIRST);
        List<VersionOption> out = new ArrayList<>();
        for (String mc : sorted) out.add(new VersionOption(mc, "release", ""));
        return out;
    }

    @Override
    public List<BuildOption> builds(String mcVersion) {
        List<String> list = new ArrayList<>();
        for (String v : allVersions()) if (mcVersion.equals(toMinecraft(v))) list.add(v);
        if (list.isEmpty()) throw new BusinessException("NeoForge 沒有 Minecraft " + mcVersion + " 的版本");
        list.sort((a, b) -> McVersions.compare(b, a));
        List<BuildOption> out = new ArrayList<>();
        for (String v : list) {
            boolean beta = v.contains("-");
            out.add(new BuildOption(v, v, beta ? "beta" : "stable"));
        }
        return out;
    }

    @Override
    public Artifact resolve(String mcVersion, String build) {
        if (!mcVersion.equals(toMinecraft(build)) || !allVersions().contains(build)) {
            throw new BusinessException("找不到 NeoForge 版本 " + build);
        }
        return new Artifact(MAVEN + "/" + build + "/neoforge-" + build + "-installer.jar",
                "neoforge-" + build + "-installer.jar", null, null, -1);
    }
}
