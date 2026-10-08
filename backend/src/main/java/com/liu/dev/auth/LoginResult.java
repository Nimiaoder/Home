package com.liu.dev.auth;

/** 登入成功後的 detail 內容 */
public record LoginResult(String token, long expiresIn, UserInfo user) {}
