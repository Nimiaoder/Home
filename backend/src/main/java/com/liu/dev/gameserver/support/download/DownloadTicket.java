package com.liu.dev.gameserver.support.download;

/** 發票券 API 的回傳內容。前端以 /api/dl/{ticket} 下載。 */
public record DownloadTicket(String ticket, String fileName) {}
