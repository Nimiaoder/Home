package com.liu.dev.gameserver.support.path;

import com.liu.dev.common.BusinessException;

import java.nio.file.Path;
import java.util.regex.Pattern;

/** 檔名 / 路徑安全檢查。所有來自使用者的檔名、資料夾名都必須先經過這裡。 */
public final class SafePaths {

    /** 允許：任何語言的文字、數字、空白，以及 _ - . ( ) [ ] + */
    private static final Pattern NAME = Pattern.compile("^[\\p{L}\\p{N}_\\-. ()\\[\\]+]{1,100}$");

    private SafePaths() {}

    /** 驗證並回傳「單一層」名稱（不可含路徑分隔符、不可以 . 開頭、不可含 ..）。 */
    public static String requireName(String raw, String label) {
        String n = raw == null ? "" : raw.trim();
        if (n.isEmpty()) throw new BusinessException(label + "不可為空");
        if (!NAME.matcher(n).matches() || n.startsWith(".") || n.contains("..") || n.endsWith(".")) {
            throw new BusinessException(label + "含有不允許的字元（僅限文字、數字、空白與 _ - . ( ) [ ] +）");
        }
        return n;
    }

    public static boolean isValidName(String raw) {
        try {
            requireName(raw, "名稱");
            return true;
        } catch (BusinessException e) {
            return false;
        }
    }

    /** 把 parts 接在 base 後面，並確保結果仍在 base 之內（防止 ../ 跳脫）。 */
    public static Path resolveInside(Path base, String... parts) {
        Path root = base.toAbsolutePath().normalize();
        Path r = root;
        for (String s : parts) r = r.resolve(s);
        r = r.normalize();
        if (!r.startsWith(root)) throw new BusinessException("路徑不合法");
        return r;
    }
}
