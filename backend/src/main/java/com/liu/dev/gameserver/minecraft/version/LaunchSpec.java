package com.liu.dev.gameserver.minecraft.version;

import java.util.List;

/** 啟動參數中「JVM 記憶體與自訂參數」之後的部分，例如 [-jar, server.jar, nogui]。 */
public record LaunchSpec(List<String> args) {}
