package com.liu.dev.gameserver.minecraft.runtime;

/** 主控台的一行輸出。seq 在同一個伺服器內遞增。 */
public record ConsoleLine(long seq, String text) {}
