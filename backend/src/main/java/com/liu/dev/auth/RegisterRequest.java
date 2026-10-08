package com.liu.dev.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "請輸入帳號")
        @Size(min = 3, max = 50, message = "帳號長度需為 3~50 字元")
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "帳號僅能使用英數字與 _ . -")
        String username,
        @NotBlank(message = "請輸入密碼")
        @Size(min = 6, max = 72, message = "密碼長度需為 6~72 字元")
        String password,
        @Size(max = 50, message = "暱稱最多 50 字元")
        String nickname) {
}
