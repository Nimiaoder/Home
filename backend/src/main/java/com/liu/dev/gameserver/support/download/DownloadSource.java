package com.liu.dev.gameserver.support.download;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** 一個可被下載的內容（檔案、即時壓縮串流……）。 */
public interface DownloadSource {

    String fileName();

    default String contentType() {
        return "application/octet-stream";
    }

    /** 已知長度回傳 bytes，未知（例如即時壓縮）回傳 -1。 */
    default long contentLength() {
        return -1;
    }

    void writeTo(OutputStream out) throws IOException;

    /** 以既有檔案建立下載來源。 */
    static DownloadSource ofFile(Path file, String downloadName) {
        return new DownloadSource() {
            @Override
            public String fileName() {
                return downloadName;
            }

            @Override
            public long contentLength() {
                try {
                    return Files.size(file);
                } catch (IOException e) {
                    return -1;
                }
            }

            @Override
            public void writeTo(OutputStream out) throws IOException {
                Files.copy(file, out);
            }
        };
    }
}
