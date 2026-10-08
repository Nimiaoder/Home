package com.liu.dev.gameserver.minecraft.version;

/**
 * 支援的伺服器核心類型。新增類型：在這裡加一個列舉值 + 實作一個 ServerProvider（@Component）即可。
 */
public enum ServerType {
    VANILLA("Vanilla", ContentKind.NONE, null, null),
    PAPER("Paper", ContentKind.PLUGIN, "plugins", "paper"),
    FABRIC("Fabric", ContentKind.MOD, "mods", "fabric"),
    FORGE("Forge", ContentKind.MOD, "mods", "forge"),
    NEOFORGE("NeoForge", ContentKind.MOD, "mods", "neoforge");

    /** 這種核心擴充內容的種類。 */
    public enum ContentKind {NONE, MOD, PLUGIN}

    private final String displayName;
    private final ContentKind contentKind;
    private final String contentDir;
    private final String modrinthLoader;

    ServerType(String displayName, ContentKind contentKind, String contentDir, String modrinthLoader) {
        this.displayName = displayName;
        this.contentKind = contentKind;
        this.contentDir = contentDir;
        this.modrinthLoader = modrinthLoader;
    }

    public String displayName() {
        return displayName;
    }

    public ContentKind contentKind() {
        return contentKind;
    }

    /** 模組 / 插件資料夾名稱（Vanilla 為 null）。 */
    public String contentDir() {
        return contentDir;
    }

    /** Modrinth 上對應的 loader 名稱（Vanilla 為 null）。 */
    public String modrinthLoader() {
        return modrinthLoader;
    }
}
