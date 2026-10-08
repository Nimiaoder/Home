package com.liu.dev.gameserver.minecraft.version;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 版本字串工具：同時支援舊制 1.20.4 與 2026 起的 26.1 / 26.1.2。 */
public final class McVersions {

    private static final Pattern CORE = Pattern.compile("^(\\d+(?:\\.\\d+)*)");

    /** 新的在前。 */
    public static final Comparator<String> NEWEST_FIRST = (a, b) -> compare(b, a);

    private McVersions() {}

    /** 取出開頭的數字部分，例如 "1.20.1-rc1" → [1,20,1]；沒有數字回傳空陣列。 */
    public static int[] numbers(String v) {
        if (v == null) return new int[0];
        Matcher m = CORE.matcher(v.trim());
        if (!m.find()) return new int[0];
        String[] parts = m.group(1).split("\\.");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                out[i] = Integer.parseInt(parts[i]);
            } catch (NumberFormatException e) {
                out[i] = Integer.MAX_VALUE;
            }
        }
        return out;
    }

    /** 由小到大比較。數字相同時，沒有後綴的正式版大於預覽版（1.21 > 1.21-rc1）。 */
    public static int compare(String a, String b) {
        int[] x = numbers(a);
        int[] y = numbers(b);
        int n = Math.max(x.length, y.length);
        for (int i = 0; i < n; i++) {
            int xi = i < x.length ? x[i] : 0;
            int yi = i < y.length ? y[i] : 0;
            if (xi != yi) return Integer.compare(xi, yi);
        }
        if (x.length != y.length) return Integer.compare(x.length, y.length);
        boolean sa = a != null && a.contains("-");
        boolean sb = b != null && b.contains("-");
        if (sa != sb) return sa ? -1 : 1;
        return 0;
    }

    /** URL path 用的編碼（空白轉 %20）。 */
    public static String enc(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
    }
}
