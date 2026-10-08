package com.liu.dev.common;

/**
 * 統一回傳格式：{ status: 1成功/0失敗, message: 訊息, detail: 任意型態結果 }
 * detail 為泛型，可放 String / Map / List / 物件 / null。
 */
public record ApiResponse<T>(int status, String message, T detail) {

    public static final int FAIL = 0;
    public static final int SUCCESS = 1;

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(SUCCESS, "", null);
    }

    public static <T> ApiResponse<T> ok(T detail) {
        return new ApiResponse<>(SUCCESS, "", detail);
    }

    public static <T> ApiResponse<T> ok(String message, T detail) {
        return new ApiResponse<>(SUCCESS, message, detail);
    }

    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(FAIL, message, null);
    }

    public static <T> ApiResponse<T> fail(String message, T detail) {
        return new ApiResponse<>(FAIL, message, detail);
    }
}
