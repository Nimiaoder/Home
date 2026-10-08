package com.liu.dev.gameserver.minecraft.mod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 從 jar 內讀取模組 / 插件資訊（Fabric、Quilt、Forge、NeoForge、Bukkit/Paper）。讀不到就回傳全空。 */
final class ModMetadataReader {

    /** 名稱、版本、識別 id（任一欄位都可能為 null）。 */
    record Meta(String name, String version, String id) {
        static final Meta EMPTY = new Meta(null, null, null);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern TOML_ID = Pattern.compile("(?m)^\\s*modId\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern TOML_VERSION = Pattern.compile("(?m)^\\s*version\\s*=\\s*\"([^\"]+)\"");
    private static final Pattern TOML_NAME = Pattern.compile("(?m)^\\s*displayName\\s*=\\s*\"([^\"]+)\"");

    private ModMetadataReader() {}

    static Meta read(Path jar) {
        try (ZipFile zf = new ZipFile(jar.toFile())) {
            ZipEntry e = zf.getEntry("fabric.mod.json");
            if (e != null) {
                JsonNode n = MAPPER.readTree(read(zf, e));
                return new Meta(text(n, "name"), text(n, "version"), text(n, "id"));
            }
            e = zf.getEntry("quilt.mod.json");
            if (e != null) {
                JsonNode n = MAPPER.readTree(read(zf, e));
                JsonNode loader = n.path("quilt_loader");
                return new Meta(text(n.path("metadata"), "name"), text(loader, "version"), text(loader, "id"));
            }
            for (String toml : new String[]{"META-INF/neoforge.mods.toml", "META-INF/mods.toml"}) {
                e = zf.getEntry(toml);
                if (e != null) {
                    String t = read(zf, e);
                    int mods = t.indexOf("[[mods]]");
                    String block = mods >= 0 ? t.substring(mods) : t;
                    return new Meta(first(TOML_NAME, block), first(TOML_VERSION, block), first(TOML_ID, block));
                }
            }
            for (String yml : new String[]{"paper-plugin.yml", "plugin.yml", "bungee.yml"}) {
                e = zf.getEntry(yml);
                if (e != null) {
                    String t = read(zf, e);
                    String name = yamlValue(t, "name");
                    return new Meta(name, yamlValue(t, "version"), name);
                }
            }
        } catch (Exception ignored) {
            // 不是標準模組或檔案損毀：顯示檔名即可
        }
        return Meta.EMPTY;
    }

    private static String read(ZipFile zf, ZipEntry e) throws Exception {
        try (InputStream in = zf.getInputStream(e)) {
            return new String(in.readNBytes(512 * 1024), StandardCharsets.UTF_8);
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.path(field);
        return v.isTextual() ? v.asText() : null;
    }

    private static String first(Pattern p, String s) {
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    private static String yamlValue(String yaml, String key) {
        Matcher m = Pattern.compile("(?m)^" + key + "\\s*:\\s*(.+)$").matcher(yaml);
        if (!m.find()) return null;
        return m.group(1).trim().replaceAll("^['\"]|['\"]$", "");
    }
}
