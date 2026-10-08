package com.liu.dev.gameserver.minecraft.version;

/** 某個遊戲版本底下的建置（Paper build、Fabric loader、Forge 版本……）。channel：stable / recommended / latest / beta */
public record BuildOption(String id, String label, String channel) {}
