package com.example.springclaudeturtorial.phase5.web.exception;

/**
 * Ném ra khi vi phạm business rule (ví dụ: tên product đã tồn tại).
 * Sẽ được map → HTTP 409 Conflict bởi GlobalExceptionHandler.
 */
public class BusinessException extends RuntimeException {

    private final String errorCode;

    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() { return errorCode; }
}
