package com.liu.dev.gameserver.support.download;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.liu.dev.common.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * 以票券下載：GET /api/dl/{ticket}。
 * 此路徑在 SecurityConfig 設為公開——票券本身就是憑證（一次性、2 分鐘有效）。
 */
@RestController
@RequestMapping("/api/dl")
public class DownloadController {

    private static final Logger log = LoggerFactory.getLogger(DownloadController.class);

    private final DownloadService downloads;
    private final ObjectMapper mapper;

    public DownloadController(DownloadService downloads, ObjectMapper mapper) {
        this.downloads = downloads;
        this.mapper = mapper;
    }

    @GetMapping("/{ticket}")
    public void download(@PathVariable String ticket, HttpServletResponse res) throws IOException {
        log.debug("*****DownloadController.download*****");
        Optional<DownloadSource> found = downloads.consume(ticket);
        if (found.isEmpty()) {
            res.setStatus(404);
            res.setContentType("application/json;charset=UTF-8");
            mapper.writeValue(res.getWriter(), ApiResponse.fail("下載連結已失效，請回到頁面重新操作"));
            return;
        }
        DownloadSource src = found.get();
        res.setContentType(src.contentType());
        if (src.contentLength() >= 0) res.setContentLengthLong(src.contentLength());
        res.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                ContentDisposition.attachment().filename(src.fileName(), StandardCharsets.UTF_8).build().toString());
        res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        src.writeTo(res.getOutputStream());
        res.flushBuffer();
    }
}
