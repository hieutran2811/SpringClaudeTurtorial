package com.example.springclaudeturtorial.phase5.web.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * ============================================================
 * PHASE 5 — Global Exception Handling
 * ============================================================
 *
 * @ControllerAdvice: áp dụng cho TẤT CẢ @RestController trong app.
 *
 * Thứ tự Spring chọn handler:
 *   1. Handler khớp exception cụ thể nhất (most specific)
 *   2. Nếu hòa → ưu tiên handler trong class gần nhất với controller
 *
 * Kế thừa ResponseEntityExceptionHandler để tái sử dụng xử lý
 * các Spring MVC exceptions mặc định (400, 405, 415, ...).
 * ============================================================
 */
@RestControllerAdvice                          // = @ControllerAdvice + @ResponseBody
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 1. ResourceNotFoundException → 404 ────────────────────────────────
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ApiError body = ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "RESOURCE_NOT_FOUND",
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // ── 2. BusinessException → 409 Conflict ───────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        log.warn("Business rule violation [{}]: {}", ex.getErrorCode(), ex.getMessage());

        ApiError body = ApiError.of(
                HttpStatus.CONFLICT.value(),
                ex.getErrorCode(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // ── 3. @Valid thất bại → 400 với chi tiết từng field ─────────────────
    //
    // Override method từ ResponseEntityExceptionHandler thay vì dùng
    // @ExceptionHandler, vì Spring MVC đã registered handler này rồi.
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {

        // Thu thập lỗi từng field
        List<ApiError.FieldError> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ApiError body = ApiError.ofValidation(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_FAILED",
                "Request validation failed — see 'errors' for details",
                fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    // ── 4. Catch-all → 500 (không để lộ stack trace ra ngoài) ────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex) {
        log.error("Unhandled exception", ex);  // log full stack trace phía server

        ApiError body = ApiError.of(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "An unexpected error occurred. Please try again later."
        );
        return ResponseEntity.internalServerError().body(body);
    }
}
