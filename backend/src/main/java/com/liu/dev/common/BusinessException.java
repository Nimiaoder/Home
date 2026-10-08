package com.liu.dev.common;

/** 業務例外：丟出後由 GlobalExceptionHandler 轉成 status=0 的統一格式。 */
public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
}
