package com.example.springclaudeturtorial.phase5.web.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Chuẩn hóa error response body cho toàn bộ API.
 *
 * Ví dụ JSON trả về:
 * {
 *   "status":  404,
 *   "code":    "RESOURCE_NOT_FOUND",
 *   "message": "Product not found with id: 99",
 *   "timestamp": "2026-03-31T10:00:00Z",
 *   "errors":  null          ← chỉ xuất hiện khi có validation errors
 * }
 */
@JsonInclude(JsonInclude.Include.NON_NULL)  // bỏ qua field null trong JSON
public record ApiError(
        int status,
        String code,
        String message,
        Instant timestamp,
        List<FieldError> errors          // chỉ dùng cho validation errors
) {
    // Convenience factory — dùng cho hầu hết trường hợp
    public static ApiError of(int status, String code, String message) {
        return new ApiError(status, code, message, Instant.now(), null);
    }

    // Convenience factory — dùng khi có field-level validation errors
    public static ApiError ofValidation(int status, String code, String message,
                                        List<FieldError> errors) {
        return new ApiError(status, code, message, Instant.now(), errors);
    }

    /**
     * Chi tiết lỗi từng field khi @Valid thất bại.
     * Ví dụ: { "field": "price", "message": "Price must be positive" }
     */
    public record FieldError(String field, String message) {}
}
