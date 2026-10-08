package com.liu.dev.gameserver.support.io;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** 檔案系統小工具（不依賴 Spring，可單獨測試）。 */
public final class FileTool {

    private FileTool() {}

    /** 遞迴計算資料夾大小（bytes）；讀不到的檔案直接略過。 */
    public static long dirSize(Path dir) {
        if (dir == null || !Files.isDirectory(dir)) return 0;
        AtomicLong total = new AtomicLong();
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (attrs.isRegularFile()) total.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ignored) {
            // 回傳目前累計值即可
        }
        return total.get();
    }

    /** 遞迴刪除（不跟隨符號連結）。不存在視為成功。 */
    public static void deleteRecursively(Path path) throws IOException {
        if (path == null || !Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return;
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** 讀檔案最後 maxLines 行（UTF-8）。檔案不存在回傳空 List。 */
    public static List<String> tailLines(Path file, int maxLines) {
        if (file == null || !Files.isRegularFile(file)) return List.of();
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            long len = raf.length();
            int toRead = (int) Math.min(len, 256L * 1024);
            raf.seek(len - toRead);
            byte[] buf = new byte[toRead];
            raf.readFully(buf);
            String text = new String(buf, StandardCharsets.UTF_8);
            List<String> lines = new ArrayList<>(Arrays.asList(text.split("\r?\n", -1)));
            if (len > toRead && !lines.isEmpty()) lines.remove(0);        // 第一行可能被截斷
            while (!lines.isEmpty() && lines.get(lines.size() - 1).isEmpty()) lines.remove(lines.size() - 1);
            int from = Math.max(0, lines.size() - maxLines);
            return new ArrayList<>(lines.subList(from, lines.size()));
        } catch (IOException e) {
            return List.of();
        }
    }

    /** 搬移並覆蓋；跨檔案系統時自動退回一般搬移。 */
    public static void moveReplace(Path src, Path dst) throws IOException {
        Files.createDirectories(dst.toAbsolutePath().getParent());
        try {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(src, dst, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /** 轉成人類可讀的錯誤文字（避免 null）。 */
    public static String msg(Throwable e) {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }
}
