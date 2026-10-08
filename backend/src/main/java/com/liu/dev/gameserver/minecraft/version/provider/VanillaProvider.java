package com.liu.dev.gameserver.minecraft.version.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.version.*;
import com.liu.dev.gameserver.support.cache.TtlCache;
import com.liu.dev.gameserver.support.http.HttpFetcher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Mojang 官方伺服器。資料來源：Mojang 版本清單（version_manifest_v2）。 */
@Component
public class VanillaProvider extends AbstractJarProvider {

    private static final String MANIFEST = "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json";

    private final HttpFetcher http;
    private final TtlCache<String, JsonNode> cache = new TtlCache<>(10 * 60 * 1000L);

    public VanillaProvider(HttpFetcher http) {
        this.http = http;
    }

    @Override
    public ServerType type() {
        return ServerType.VANILLA;
    }

    private JsonNode manifest() {
        return cache.get("manifest", () -> http.getJson(MANIFEST));
    }

    @Override
    public List<VersionOption> versions() {
        List<VersionOption> out = new ArrayList<>();
        for (JsonNode v : manifest().path("versions")) {
            String kind = v.path("type").asText();
            if (!kind.equals("release") && !kind.equals("snapshot")) continue;   // 略過 old_alpha / old_beta
            out.add(new VersionOption(v.path("id").asText(), kind, v.path("releaseTime").asText("")));
        }
        return out;                                                              // 清單本身就是新的在前
    }

    @Override
    public List<BuildOption> builds(String mcVersion) {
        return List.of(new BuildOption("default", "官方版本", "stable"));
    }

    @Override
    public Artifact resolve(String mcVersion, String build) {
        String metaUrl = null;
        for (JsonNode v : manifest().path("versions")) {
            if (mcVersion.equals(v.path("id").asText())) {
                metaUrl = v.path("url").asText();
                break;
            }
        }
        if (metaUrl == null) throw new BusinessException("找不到 Minecraft 版本 " + mcVersion);
        JsonNode server = http.getJson(metaUrl).path("downloads").path("server");
        if (server.isMissingNode() || server.path("url").asText("").isEmpty()) {
            throw new BusinessException("Minecraft " + mcVersion + " 沒有提供伺服器檔案");
        }
        return new Artifact(server.path("url").asText(), "minecraft_server." + mcVersion + ".jar",
                "sha1", server.path("sha1").asText(null), server.path("size").asLong(-1));
    }
}
