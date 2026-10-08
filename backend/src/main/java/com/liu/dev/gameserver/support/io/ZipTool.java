package com.liu.dev.gameserver.support.io;

import com.liu.dev.common.BusinessException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** 壓縮 / 解壓縮（含 zip-slip 防護、Windows 中文檔名 GBK/Big5 退回）。 */
public final class ZipTool {

    private static final List<Charset> CHARSETS = new ArrayList<>();

    static {
        CHARSETS.add(StandardCharsets.UTF_8);
        for (String name : new String[]{"GBK", "Big5"}) {
            try {
                CHARSETS.add(Charset.forName(name));
            } catch (Exception ignored) {
                // 精簡版 JRE 可能沒有這些字元集，略過
            }
        }
    }

    private ZipTool() {}

    /** 開啟 zip；UTF-8 失敗時依序嘗試 GBK、Big5。呼叫端負責 close。 */
    public static ZipFile open(Path zip) throws IOException {
        IOException last = null;
        for (Charset cs : CHARSETS) {
            try {
                return new ZipFile(zip.toFile(), cs);
            } catch (ZipException | IllegalArgumentException e) {
                last = e instanceof IOException io ? io : new IOException(e);
            }
        }
        throw last != null ? last : new IOException("無法開啟壓縮檔");
    }

    /** 統一路徑分隔符。 */
    public static String norm(String entryName) {
        return entryName.replace('\\', '/');
    }

    private static boolean ignored(String name) {
        return name.startsWith("__MACOSX/") || name.contains("/__MACOSX/")
                || name.endsWith(".DS_Store") || name.endsWith("/session.lock") || name.equals("session.lock");
    }

    /** 找出含 level.dat 的最淺層目錄前綴（"" 表示 zip 根目錄，"abc/" 表示子資料夾）；找不到回傳 null。 */
    public static String findWorldRoot(ZipFile zf) {
        String best = null;
        int bestDepth = Integer.MAX_VALUE;
        Enumeration<? extends ZipEntry> en = zf.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            if (e.isDirectory()) continue;
            String n = norm(e.getName());
            if (ignored(n)) continue;
            if (!n.equals("level.dat") && !n.endsWith("/level.dat")) continue;
            String prefix = n.substring(0, n.length() - "level.dat".length());
            int depth = (int) prefix.chars().filter(c -> c == '/').count();
            if (depth > 3) continue;
            if (depth < bestDepth || (depth == bestDepth && prefix.compareTo(best) < 0)) {
                best = prefix;
                bestDepth = depth;
            }
        }
        return best;
    }

    /** 是否有任何 entry 位於 prefix 之下。 */
    public static boolean hasPrefix(ZipFile zf, String prefix) {
        Enumeration<? extends ZipEntry> en = zf.entries();
        while (en.hasMoreElements()) {
            if (norm(en.nextElement().getName()).startsWith(prefix)) return true;
        }
        return false;
    }

    /** 把 zip 內 prefix 之下的所有內容解壓到 destDir（去掉 prefix）。 */
    public static void extractPrefix(ZipFile zf, String prefix, Path destDir) throws IOException {
        Path root = destDir.toAbsolutePath().normalize();
        Files.createDirectories(root);
        Enumeration<? extends ZipEntry> en = zf.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            String n = norm(e.getName());
            if (!n.startsWith(prefix) || ignored(n)) continue;
            String rel = n.substring(prefix.length());
            if (rel.isEmpty()) continue;
            Path dest = root.resolve(rel).normalize();
            if (!dest.startsWith(root)) throw new BusinessException("壓縮檔含有不安全的路徑，已中止");
            if (e.isDirectory() || n.endsWith("/")) {
                Files.createDirectories(dest);
            } else {
                Files.createDirectories(dest.getParent());
                try (InputStream in = zf.getInputStream(e)) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    /**
     * 把 baseDir 底下的 topDirs 壓成 zip（zip 內第一層就是各資料夾名稱）。
     * 會關閉 out。壓縮等級用最快，因為地圖的 .mca 本來就已經壓縮過。
     */
    public static void zipDirs(Path baseDir, List<String> topDirs, OutputStream out) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.setLevel(Deflater.BEST_SPEED);
            for (String top : topDirs) {
                Path dir = baseDir.resolve(top);
                if (!Files.isDirectory(dir)) continue;
                Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes attrs) throws IOException {
                        String rel = dir.relativize(d).toString().replace('\\', '/');
                        String name = rel.isEmpty() ? top + "/" : top + "/" + rel + "/";
                        zos.putNextEntry(new ZipEntry(name));
                        zos.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path f, BasicFileAttributes attrs) throws IOException {
                        if (!attrs.isRegularFile() || f.getFileName().toString().equals("session.lock")) {
                            return FileVisitResult.CONTINUE;
                        }
                        ZipEntry ze = new ZipEntry(top + "/" + dir.relativize(f).toString().replace('\\', '/'));
                        ze.setTime(attrs.lastModifiedTime().toMillis());
                        zos.putNextEntry(ze);
                        try {
                            Files.copy(f, zos);
                        } catch (IOException ex) {
                            // 檔案在壓縮途中被伺服器刪除/鎖定：略過該檔，不中斷整個壓縮
                        }
                        zos.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFileFailed(Path f, IOException exc) {
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
    }
}
