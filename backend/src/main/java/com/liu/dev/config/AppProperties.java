package com.liu.dev.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** 對應 application.yml 的 app.* 設定。 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Admin admin, Cors cors, Minecraft minecraft) {
    public record Jwt(String secret, long expireMinutes) {}
    public record Admin(String username, String password, String nickname) {}
    public record Cors(List<String> origins) {}

    /**
     * Minecraft 伺服器管理設定。
     *
     * @param baseDir            所有檔案的根目錄（伺服器、版本庫、備份、Java）
     * @param stopTimeoutSeconds 送出 stop 後，等多久才強制結束
     * @param consoleBufferLines 每個伺服器在記憶體保留的主控台行數
     * @param userAgent          呼叫 Paper / Modrinth 等外部 API 時使用的 User-Agent（建議改成自己的聯絡方式）
     */
    public record Minecraft(String baseDir, Integer stopTimeoutSeconds,
                            Integer consoleBufferLines, String userAgent) {}
}
