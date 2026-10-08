package com.liu.dev.gameserver.minecraft.mod.modrinth;

import com.fasterxml.jackson.databind.JsonNode;
import com.liu.dev.gameserver.minecraft.version.McVersions;
import com.liu.dev.gameserver.support.http.HttpFetcher;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/** Modrinth API v2 的薄包裝（只做 HTTP 與參數組裝，不含業務邏輯）。 */
@Component
public class ModrinthClient {

    private static final String API = "https://api.modrinth.com/v2";

    private final HttpFetcher http;

    public ModrinthClient(HttpFetcher http) {
        this.http = http;
    }

    public JsonNode search(String query, String facetsJson, int offset, int limit) {
        String index = query == null || query.isBlank() ? "downloads" : "relevance";
        return http.getJson(API + "/search?query=" + McVersions.enc(query == null ? "" : query)
                + "&facets=" + McVersions.enc(facetsJson)
                + "&index=" + index + "&limit=" + limit + "&offset=" + offset);
    }

    /** 專案在指定 loader 與遊戲版本下的所有版本（新的在前）。 */
    public JsonNode versions(String project, List<String> loaders, String mcVersion) {
        return http.getJson(API + "/project/" + McVersions.enc(project) + "/version?loaders="
                + McVersions.enc(jsonArray(loaders)) + "&game_versions=" + McVersions.enc(jsonArray(List.of(mcVersion))));
    }

    public JsonNode project(String project) {
        return http.getJson(API + "/project/" + McVersions.enc(project));
    }

    public void download(String url, Path target, String sha1, BiConsumer<Long, Long> progress) {
        http.download(url, target, sha1 == null ? null : "sha1", sha1, progress);
    }

    private static String jsonArray(List<String> items) {
        return items.stream().map(s -> "\"" + s.replace("\"", "") + "\"").collect(Collectors.joining(",", "[", "]"));
    }
}
