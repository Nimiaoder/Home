package com.liu.dev.gameserver.minecraft.java;

import com.liu.dev.gameserver.minecraft.version.McVersions;

/** 各 Minecraft 版本需要的「最低」Java 版本。 */
public final class JavaRequirement {

    private JavaRequirement() {}

    public static int forMinecraft(String mcVersion) {
        int[] v = McVersions.numbers(mcVersion);
        if (v.length == 0) return 21;
        if (v[0] >= 26) return 25;                       // 2026 起：26.1+ 需要 Java 25
        if (v[0] != 1) return 21;                        // 舊式快照名稱，如 25w14a
        int minor = v.length > 1 ? v[1] : 0;
        int patch = v.length > 2 ? v[2] : 0;
        if (minor >= 21) return 21;
        if (minor == 20 && patch >= 5) return 21;        // 1.20.5+
        if (minor >= 17) return 17;                      // 1.17 ~ 1.20.4（1.17 最低 16，17 可用）
        return 8;
    }
}
