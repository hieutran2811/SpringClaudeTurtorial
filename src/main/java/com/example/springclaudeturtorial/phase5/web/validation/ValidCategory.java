package com.example.springclaudeturtorial.phase5.web.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * ============================================================
 * PHASE 5 — Custom Constraint Annotation
 * ============================================================
 *
 * Custom constraint hoạt động như @NotBlank, @Size, ... nhưng do mình định nghĩa.
 *
 * 3 thành phần bắt buộc:
 *   - message()   : thông báo lỗi mặc định
 *   - groups()    : để dùng với Validation Groups (xem CreateProductRequest)
 *   - payload()   : metadata mở rộng (thường để trống)
 *
 * @Constraint(validatedBy = ...) → chỉ định class thực hiện logic validate
 * ============================================================
 */
@Documented
@Constraint(validatedBy = ValidCategoryValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCategory {

    String message() default "Category must be one of: Electronics, Books, Clothing, Food, Other";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
