package com.liu.dev.auth;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/** 回給前端的使用者資訊（不含密碼） */
public record UserInfo(
        String username,
        String nickname,
        String role,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime createdAt) {
}
