package com.liu.dev.gameserver.minecraft.version.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.version.*;
import com.liu.dev.gameserver.support.cache.TtlCache;
import com.liu.dev.gameserver.support.http.HttpFetcher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Paper（支援 Bukkit / Spigot 插件）。資料來源：PaperMC Fill v3 API。
 * 注意：舊的 api.papermc.io (v2) 已於 2026-07-01 停用，必須使用 fill.papermc.io。
 */
@Component
public class PaperProvider extends AbstractJarProvider {

    private static final String API = "https://fill.papermc.io/v3/projects/paper";

    private final HttpFetcher http;
    private final TtlCache<String, JsonNode> cache = new TtlCache<>(10 * 60 * 1000L);

    public PaperProvider(HttpFetcher http) {
        this.http = http;
    }

    @Override
    public ServerType type() {
        return ServerType.PAPER;
    }

    @Override
    public List<VersionOption> versions() {
        JsonNode versions = cache.get("project", () -> http.getJson(API)).path("versions");
        Set<String> ids = new LinkedHashSet<>();
        if (versions.isObject()) {
            // 形如 { "26.1": ["26.1.2", "26.1.1"], "1.21": [...] }，順序即為新到舊
            Iterator<Map.Entry<String, JsonNode>> it = versions.fields();
            while (it.hasNext()) {
                for (JsonNode v : it.next().getValue()) ids.add(v.asText());
            }
        } else if (versions.isArray()) {
            for (JsonNode v : versions) ids.add(v.asText());
        }
        List<VersionOption> out = new ArrayList<>();
        for (String id : ids) out.add(new VersionOption(id, id.contains("-") ? "snapshot" : "release", ""));
        return out;
    }

    private JsonNode buildsJson(String mcVersion) {
        JsonNode arr = cache.get("builds:" + mcVersion,
                () -> http.getJson(API + "/versions/" + McVersions.enc(mcVersion) + "/builds"));
        if (!arr.isArray()) {
            throw new BusinessException("Paper 沒有 " + mcVersion + " 的建置：" + arr.path("message").asText("未知原因"));
        }
        return arr;
    }

    @Override
    public List<BuildOption> builds(String mcVersion) {
        List<BuildOption> out = new ArrayList<>();
        for (JsonNode b : buildsJson(mcVersion)) {
            String channel = b.path("channel").asText("STABLE").toLowerCase();
            String id = b.path("id").asText();
            out.add(new BuildOption(id, "#" + id + ("stable".equals(channel) ? "" : "（" + channel + "）"), channel));
        }
        return out;
    }

    @Override
    public Artifact resolve(String mcVersion, String build) {
        for (JsonNode b : buildsJson(mcVersion)) {
            if (!build.equals(b.path("id").asText())) continue;
            JsonNode d = b.path("downloads").path("server:default");
            if (d.isMissingNode()) throw new BusinessException("此 Paper 建置沒有伺服器檔案");
            return new Artifact(d.path("url").asText(), d.path("name").asText("paper-" + mcVersion + "-" + build + ".jar"),
                    "sha256", d.path("checksums").path("sha256").asText(null), d.path("size").asLong(-1));
        }
        throw new BusinessException("找不到 Paper " + mcVersion + " 的建置 #" + build);
    }
}
