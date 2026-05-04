package com.library.catalog.application.dto;

public record ApiResponse<T>(
    boolean success,
    T data,
    String error,
    String errorCode
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> error(String errorCode, String error) {
        return new ApiResponse<>(false, null, error, errorCode);
    }
}
