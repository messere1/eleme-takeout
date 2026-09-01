package cn.edu.tju.takeout.common;

import java.util.UUID;

public record ApiResponse<T>(Object code, String msg, T data, String traceId) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data, newTraceId());
    }

    public static <T> ApiResponse<T> failure(String code, String message, T data) {
        return new ApiResponse<>(code, message, data, newTraceId());
    }

    private static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}

