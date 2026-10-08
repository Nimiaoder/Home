package com.liu.dev.gameserver.minecraft.version.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.version.*;
import com.liu.dev.gameserver.support.cache.TtlCache;
import com.liu.dev.gameserver.support.http.HttpFetcher;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Minecraft Forge。資料來源：Forge Maven（maven-metadata.xml）與 promotions_slim.json。 */
@Component
public class ForgeProvider extends InstallerProvider {

    private static final String MAVEN = "https://maven.minecraftforge.net/net/minecraftforge/forge";
    private static final String PROMOS = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json";
    private static final Pattern VERSION_TAG = Pattern.compile("<version>([^<]+)</version>");

    private final HttpFetcher http;
    private final TtlCache<String, List<String>> idCache = new TtlCache<>(10 * 60 * 1000L);
    private final TtlCache<String, Map<String, String>> promoCache = new TtlCache<>(10 * 60 * 1000L);

    public ForgeProvider(HttpFetcher http) {
        this.http = http;
    }

    @Override
    public ServerType type() {
        return ServerType.FORGE;
    }

    @Override
    protected String librariesSubPath() {
        return "libraries/net/minecraftforge/forge";
    }

    @Override
    protected String legacyJarPrefix() {
        return "forge-";
    }

    /** 全部 Forge 版本代號，例如 1.20.1-47.3.0 */
    private List<String> allIds() {
        return idCache.get("ids", () -> {
            String xml = http.getString(MAVEN + "/maven-metadata.xml");
            List<String> ids = new ArrayList<>();
            Matcher m = VERSION_TAG.matcher(xml);
            while (m.find()) ids.add(m.group(1).trim());
            if (ids.isEmpty()) throw new BusinessException("無法解析 Forge 版本清單");
            return ids;
        });
    }

    private static String mcOf(String id) {
        int i = id.indexOf('-');
        return i < 0 ? id : id.substring(0, i);
    }

    private static String forgePart(String id) {
        int i = id.indexOf('-');
        return i < 0 ? id : id.substring(i + 1);
    }

    @Override
    public List<VersionOption> versions() {
        Set<String> mcs = new LinkedHashSet<>();
        for (String id : allIds()) {
            String mc = mcOf(id);
            if (McVersions.compare(mc, "1.7.10") >= 0 && mc.matches("^\\d+(\\.\\d+)*$")) mcs.add(mc);
        }
        List<String> sorted = new ArrayList<>(mcs);
        sorted.sort(McVersions.NEWEST_FIRST);
        List<VersionOption> out = new ArrayList<>();
        for (String mc : sorted) out.add(new VersionOption(mc, "release", ""));
        return out;
    }

    private Map<String, String> promos() {
        try {
            return promoCache.get("promos", () -> {
                Map<String, String> m = new HashMap<>();
                JsonNode promos = http.getJson(PROMOS).path("promos");
                promos.fields().forEachRemaining(e -> m.put(e.getKey(), e.getValue().asText()));
                return m;
            });
        } catch (RuntimeException e) {
            return Map.of();      // 只是用來標示「推薦版」，取不到不影響使用
        }
    }

    @Override
    public List<BuildOption> builds(String mcVersion) {
        List<String> ids = new ArrayList<>();
        for (String id : allIds()) if (id.startsWith(mcVersion + "-")) ids.add(id);
        if (ids.isEmpty()) throw new BusinessException("Forge 沒有 Minecraft " + mcVersion + " 的版本");
        ids.sort((a, b) -> McVersions.compare(forgePart(b), forgePart(a)));
        Map<String, String> promos = promos();
        String recommended = promos.get(mcVersion + "-recommended");
        String latest = promos.get(mcVersion + "-latest");
        List<BuildOption> out = new ArrayList<>();
        for (String id : ids) {
            String part = forgePart(id);
            String channel = "stable";
            String label = part;
            if (recommended != null && part.startsWith(recommended)) {
                channel = "recommended";
                label += "（推薦）";
            } else if (latest != null && part.startsWith(latest)) {
                channel = "latest";
                label += "（最新）";
            }
            out.add(new BuildOption(id, label, channel));
        }
        return out;
    }

    @Override
    public Artifact resolve(String mcVersion, String build) {
        if (!allIds().contains(build)) throw new BusinessException("找不到 Forge 版本 " + build);
        return new Artifact(MAVEN + "/" + build + "/forge-" + build + "-installer.jar",
                "forge-" + build + "-installer.jar", null, null, -1);
    }
}
