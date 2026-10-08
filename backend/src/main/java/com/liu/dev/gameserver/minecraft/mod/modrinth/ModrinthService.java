package com.liu.dev.gameserver.minecraft.mod.modrinth;

import com.fasterxml.jackson.databind.JsonNode;
import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.mod.ModService;
import com.liu.dev.gameserver.minecraft.server.McServer;
import com.liu.dev.gameserver.minecraft.version.ServerType;
import com.liu.dev.gameserver.support.path.SafePaths;
import com.liu.dev.gameserver.support.task.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 從 Modrinth 搜尋並安裝模組 / 插件（自動挑選符合伺服器版本與 loader 的最新正式版，並安裝必要相依項目）。 */
@Service
public class ModrinthService {

    private static final Logger log = LoggerFactory.getLogger(ModrinthService.class);

    private static final int MAX_DEPTH = 2;

    /** 搜尋結果的一筆。 */
    public record Hit(String projectId, String slug, String title, String description,
                      String iconUrl, String author, long downloads) {}

    /** 搜尋結果。 */
    public record SearchResult(List<Hit> hits, long total) {}

    private final ModrinthClient client;
    private final ModService mods;

    public ModrinthService(ModrinthClient client, ModService mods) {
        this.client = client;
        this.mods = mods;
    }

    private static List<String> loaders(ServerType t) {
        if (t == ServerType.PAPER) return List.of("paper", "spigot", "bukkit");
        if (t.modrinthLoader() == null) throw new BusinessException("Vanilla 伺服器不支援模組或插件");
        return List.of(t.modrinthLoader());
    }

    private static String facets(ServerType t, String mc) {
        StringBuilder sb = new StringBuilder("[");
        if (t == ServerType.PAPER) {
            sb.append("[\"categories:paper\",\"categories:spigot\",\"categories:bukkit\"]");
        } else {
            sb.append("[\"project_type:mod\"],[\"categories:").append(t.modrinthLoader()).append("\"]");
            sb.append(",[\"server_side:required\",\"server_side:optional\"]");     // 排除純客戶端模組
        }
        sb.append(",[\"versions:").append(mc).append("\"]]");
        return sb.toString();
    }

    public SearchResult search(McServer s, String query, int offset) {
        ServerType t = s.getType();
        loaders(t);
        JsonNode res = client.search(query, facets(t, s.getMcVersion()), Math.max(0, offset), 20);
        List<Hit> hits = new ArrayList<>();
        for (JsonNode h : res.path("hits")) {
            hits.add(new Hit(h.path("project_id").asText(), h.path("slug").asText(), h.path("title").asText(),
                    h.path("description").asText(""), h.path("icon_url").asText(""),
                    h.path("author").asText(""), h.path("downloads").asLong(0)));
        }
        return new SearchResult(hits, res.path("total_hits").asLong(hits.size()));
    }

    /** 安裝專案（及其必要相依項目）。在背景工作中執行。 */
    public void install(McServer s, String projectId, TaskService.Context ctx) {
        Path dir = mods.contentDir(s);
        try {
            Files.createDirectories(dir);
        } catch (java.io.IOException e) {
            throw new BusinessException("無法建立資料夾 " + dir);
        }
        List<String> installed = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        installOne(s, dir, projectId, 0, visited, installed, ctx);
        if (installed.isEmpty()) throw new BusinessException("這個專案（及相依項目）已經安裝過了");
        ctx.progress(100, "已安裝：" + String.join("、", installed));
    }

    private void installOne(McServer s, Path dir, String project, int depth, Set<String> visited,
                            List<String> installed, TaskService.Context ctx) {
        if (!visited.add(project)) return;
        ctx.progress(-1, "查詢 " + project + " 的相容版本…");
        JsonNode versions = client.versions(project, loaders(s.getType()), s.getMcVersion());
        if (!versions.isArray() || versions.isEmpty()) {
            throw new BusinessException("找不到支援 " + s.getType().displayName() + " " + s.getMcVersion() + " 的「" + project + "」版本");
        }
        JsonNode chosen = versions.get(0);
        for (JsonNode v : versions) {
            if ("release".equals(v.path("version_type").asText())) {
                chosen = v;
                break;
            }
        }
        JsonNode file = null;
        for (JsonNode f : chosen.path("files")) {
            if (f.path("primary").asBoolean(false)) {
                file = f;
                break;
            }
        }
        if (file == null && chosen.path("files").isArray() && !chosen.path("files").isEmpty()) file = chosen.path("files").get(0);
        if (file == null) throw new BusinessException("此版本沒有可下載的檔案");

        String fileName = SafePaths.requireName(file.path("filename").asText(), "檔名");
        Path target = dir.resolve(fileName);
        if (Files.exists(target) || Files.exists(dir.resolve(fileName + ".disabled"))) {
            // 已安裝，仍要檢查相依
        } else {
            log.info("正在下載{}.....至{}", fileName, target);
            ctx.progress(-1, "下載 " + fileName);
            client.download(file.path("url").asText(), target, file.path("hashes").path("sha1").asText(null),
                    (done, total) -> {
                        ctx.transfer(done, total);
                        ctx.progress(total > 0 ? (int) (done * 90 / total) : -1, null);
                    });
            ctx.clearTransfer();
            installed.add(fileName);
        }

        if (depth >= MAX_DEPTH) return;
        Set<String> have = mods.installedIds(s);
        for (JsonNode d : chosen.path("dependencies")) {
            if (!"required".equals(d.path("dependency_type").asText())) continue;
            String depId = d.path("project_id").asText("");
            if (depId.isEmpty()) continue;
            JsonNode proj = client.project(depId);
            String slug = proj.path("slug").asText("").toLowerCase();
            if (have.contains(slug) || have.contains(slug.replace('-', '_'))) continue;
            installOne(s, dir, depId, depth + 1, visited, installed, ctx);
        }
    }
}
