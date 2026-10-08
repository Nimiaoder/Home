package com.liu.dev.gameserver.support.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liu.dev.common.BusinessException;
import com.liu.dev.config.AppProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.function.BiConsumer;

/** 對外 HTTP：取 JSON / 文字、下載檔案（含進度、雜湊驗證、.part 暫存檔）。 */
@Component
public class HttpFetcher {

    private static final String DEFAULT_UA = "Home-MinecraftManager/1.0";

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private final ObjectMapper mapper;
    private final String userAgent;

    public HttpFetcher(ObjectMapper mapper, AppProperties props) {
        this.mapper = mapper;
        String ua = props.minecraft() == null ? null : props.minecraft().userAgent();
        this.userAgent = StringUtils.hasText(ua) ? ua : DEFAULT_UA;
    }

    public JsonNode getJson(String url) {
        try {
            return mapper.readTree(getString(url));
        } catch (IOException e) {
            throw new BusinessException("遠端資料格式錯誤：" + host(url));
        }
    }

    public String getString(String url) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", userAgent)
                .header("Accept", "application/json, text/xml, */*")
                .timeout(Duration.ofSeconds(30))
                .GET().build();
        try {
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() / 100 != 2) {
                throw new BusinessException("遠端伺服器回應 " + res.statusCode() + "（" + host(url) + "）");
            }
            return res.body();
        } catch (IOException e) {
            throw new BusinessException("無法連線到 " + host(url) + "，請確認伺服器可連外網路");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("操作已被中斷");
        }
    }

    /**
     * 下載檔案到 target（先寫 .part，驗證通過才改名）。
     *
     * @param hashAlgo     sha1 / sha256 / sha512，null 表示不驗證
     * @param expectedHash 十六進位雜湊值
     * @param progress     (已下載, 總大小或 -1)
     */
    public void download(String url, Path target, String hashAlgo, String expectedHash,
                         BiConsumer<Long, Long> progress) {
        Path part = target.resolveSibling(target.getFileName() + ".part");
        try {
            Files.createDirectories(target.toAbsolutePath().getParent());
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .header("User-Agent", userAgent)
                    .timeout(Duration.ofMinutes(30))
                    .GET().build();
            HttpResponse<InputStream> res = client.send(req, HttpResponse.BodyHandlers.ofInputStream());
            if (res.statusCode() / 100 != 2) {
                res.body().close();
                throw new BusinessException("下載失敗：遠端伺服器回應 " + res.statusCode() + "（" + host(url) + "）");
            }
            long total = res.headers().firstValueAsLong("Content-Length").orElse(-1);
            MessageDigest md = (hashAlgo != null && StringUtils.hasText(expectedHash))
                    ? MessageDigest.getInstance(javaAlgo(hashAlgo)) : null;
            try (InputStream in = res.body(); OutputStream out = Files.newOutputStream(part)) {
                byte[] buf = new byte[64 * 1024];
                long done = 0;
                int n;
                while ((n = in.read(buf)) != -1) {
                    out.write(buf, 0, n);
                    if (md != null) md.update(buf, 0, n);
                    done += n;
                    if (progress != null) progress.accept(done, total);
                }
            }
            if (md != null && !HexFormat.of().formatHex(md.digest()).equalsIgnoreCase(expectedHash.trim())) {
                Files.deleteIfExists(part);
                throw new BusinessException("下載檔案的雜湊驗證失敗，請重試");
            }
            Files.move(part, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            deleteQuietly(part);
            throw new BusinessException("下載失敗：" + host(url) + "（" + e.getMessage() + "）");
        } catch (InterruptedException e) {
            deleteQuietly(part);
            Thread.currentThread().interrupt();
            throw new BusinessException("下載已被中斷");
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("不支援的雜湊演算法：" + hashAlgo);
        } catch (RuntimeException e) {
            deleteQuietly(part);
            throw e;
        }
    }

    private static String javaAlgo(String a) {
        return switch (a.toLowerCase().replace("-", "")) {
            case "sha1" -> "SHA-1";
            case "sha256" -> "SHA-256";
            case "sha512" -> "SHA-512";
            default -> a;
        };
    }

    private static void deleteQuietly(Path p) {
        try {
            Files.deleteIfExists(p);
        } catch (IOException ignored) {
            // ignore
        }
    }

    private static String host(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }
}
