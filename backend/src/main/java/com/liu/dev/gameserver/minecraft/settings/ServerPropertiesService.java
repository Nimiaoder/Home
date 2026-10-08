package com.liu.dev.gameserver.minecraft.settings;

import com.liu.dev.common.BusinessException;
import com.liu.dev.gameserver.minecraft.storage.MinecraftPaths;
import com.liu.dev.gameserver.support.io.FileTool;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * server.properties 讀寫：保留原檔的註解與順序，只改動指定的 key。
 * 寫入時非 ASCII 字元一律以 \\uXXXX 跳脫，這樣不論 Minecraft 版本用什麼編碼讀取都不會亂碼。
 */
@Service
public class ServerPropertiesService {

    private static final Pattern KEY = Pattern.compile("^[A-Za-z0-9._\\-]{1,64}$");

    private final MinecraftPaths paths;

    public ServerPropertiesService(MinecraftPaths paths) {
        this.paths = paths;
    }

    private Path file(String dirName) {
        return paths.serverDir(dirName).resolve("server.properties");
    }

    /** 依檔案順序回傳所有設定；檔案尚未產生回傳空 Map。 */
    public Map<String, String> read(String dirName) {
        Map<String, String> out = new LinkedHashMap<>();
        for (String line : readLines(dirName)) {
            String key = keyOf(line);
            if (key == null) continue;
            try {
                Properties p = new Properties();
                p.load(new StringReader(line));
                out.put(key, p.getProperty(key, ""));
            } catch (IOException | IllegalArgumentException e) {
                out.put(key, line.substring(line.indexOf('=') + 1));
            }
        }
        return out;
    }

    public String get(String dirName, String key, String defaultValue) {
        return read(dirName).getOrDefault(key, defaultValue);
    }

    public boolean exists(String dirName) {
        return Files.isRegularFile(file(dirName));
    }

    public void set(String dirName, String key, String value) {
        update(dirName, Map.of(key, value));
    }

    /** 更新（或新增）指定的 key。 */
    public void update(String dirName, Map<String, String> changes) {
        for (String k : changes.keySet()) {
            if (k == null || !KEY.matcher(k).matches()) throw new BusinessException("不合法的設定名稱：" + k);
        }
        List<String> lines = new ArrayList<>(readLines(dirName));
        Set<String> done = new HashSet<>();
        for (int i = 0; i < lines.size(); i++) {
            String key = keyOf(lines.get(i));
            if (key != null && changes.containsKey(key) && !done.contains(key)) {
                lines.set(i, key + "=" + escape(changes.get(key)));
                done.add(key);
            }
        }
        for (Map.Entry<String, String> e : changes.entrySet()) {
            if (!done.contains(e.getKey())) lines.add(e.getKey() + "=" + escape(e.getValue()));
        }
        try {
            Path f = file(dirName);
            Files.createDirectories(f.getParent());
            Files.write(f, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("寫入 server.properties 失敗：" + FileTool.msg(e));
        }
    }

    private List<String> readLines(String dirName) {
        Path f = file(dirName);
        if (!Files.isRegularFile(f)) return List.of();
        try {
            return Files.readAllLines(f, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException("讀取 server.properties 失敗：" + FileTool.msg(e));
        }
    }

    /** 取得這一行的 key；註解或空白行回傳 null。 */
    private static String keyOf(String line) {
        String t = line.strip();
        if (t.isEmpty() || t.startsWith("#") || t.startsWith("!")) return null;
        int eq = t.indexOf('=');
        if (eq <= 0) return null;
        return t.substring(0, eq).strip();
    }

    static String escape(String v) {
        if (v == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20 || c > 0x7e) sb.append(String.format("\\u%04X", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.toString();
    }
}
