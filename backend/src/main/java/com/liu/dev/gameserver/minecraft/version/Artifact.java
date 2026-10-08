package com.liu.dev.gameserver.minecraft.version;

/** 解析後的下載資訊。hashAlgo / hash 可為 null（遠端未提供時不驗證）。 */
public record Artifact(String url, String fileName, String hashAlgo, String hash, long size) {}
