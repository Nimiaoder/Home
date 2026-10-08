package com.liu.dev.gameserver.minecraft.version;

/** 一個可選的遊戲版本。kind：release / snapshot。 */
public record VersionOption(String id, String kind, String releasedAt) {}
