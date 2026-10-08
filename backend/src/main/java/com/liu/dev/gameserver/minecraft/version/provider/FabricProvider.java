package com.liu.dev.gameserver.minecraft.version.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.version.*;
import com.liu.dev.gameserver.support.cache.TtlCache;
import com.liu.dev.gameserver.support.http.HttpFetcher;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Fabric 模組載入器。資料來源：Fabric Meta API。build = Loader 版本，Installer 取最新穩定版。 */
@Component
public class FabricProvider extends AbstractJarProvider {

    private static final String META = "https://meta.fabricmc.net/v2/versions";

    private final HttpFetcher http;
    private final TtlCache<String, JsonNode> cache = new TtlCache<>(10 * 60 * 1000L);

    public FabricProvider(HttpFetcher http) {
        this.http = http;
    }

    @Override
    public ServerType type() {
        return ServerType.FABRIC;
    }

    @Override
    public List<VersionOption> versions() {
        List<VersionOption> out = new ArrayList<>();
        for (JsonNode v : cache.get("game", () -> http.getJson(META + "/game"))) {
            out.add(new VersionOption(v.path("version").asText(), v.path("stable").asBoolean(false) ? "release" : "snapshot", ""));
        }
        return out;
    }

    @Override
    public List<BuildOption> builds(String mcVersion) {
        JsonNode arr = cache.get("loader:" + mcVersion, () -> http.getJson(META + "/loader/" + McVersions.enc(mcVersion)));
        List<BuildOption> out = new ArrayList<>();
        for (JsonNode e : arr) {
            JsonNode l = e.path("loader");
            String v = l.path("version").asText();
            out.add(new BuildOption(v, "Loader " + v, l.path("stable").asBoolean(false) ? "stable" : "beta"));
        }
        if (out.isEmpty()) throw new BusinessException("Fabric 尚未支援 Minecraft " + mcVersion);
        return out;
    }

    private String latestInstaller() {
        JsonNode arr = cache.get("installer", () -> http.getJson(META + "/installer"));
        for (JsonNode i : arr) if (i.path("stable").asBoolean(false)) return i.path("version").asText();
        if (arr.isArray() && arr.size() > 0) return arr.get(0).path("version").asText();
        throw new BusinessException("無法取得 Fabric Installer 版本");
    }

    @Override
    public Artifact resolve(String mcVersion, String build) {
        String installer = latestInstaller();
        String url = META + "/loader/" + McVersions.enc(mcVersion) + "/" + McVersions.enc(build) + "/" + installer + "/server/jar";
        String name = "fabric-server-mc." + mcVersion + "-loader." + build + "-launcher." + installer + ".jar";
        return new Artifact(url, name, null, null, -1);
    }
}
