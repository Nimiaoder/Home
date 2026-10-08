package com.liu.dev.gameserver.minecraft.version;

import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * 安裝時給 provider 的資訊。
 *
 * @param serverDir 伺服器工作目錄
 * @param artifact  版本庫中已下載好的檔案
 * @param javaExe   可用的 java 執行檔（需要執行安裝程式的類型才會用到，可為 null）
 * @param log       回報安裝過程文字（會顯示在進度訊息）
 */
public record InstallContext(Path serverDir, Path artifact, String javaExe, Consumer<String> log) {}
