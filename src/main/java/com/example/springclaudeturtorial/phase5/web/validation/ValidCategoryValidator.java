package com.example.springclaudeturtorial.phase5.web.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

/**
 * Logic validate thực tế của @ValidCategory.
 *
 * ConstraintValidator<A, T>:
 *   A = annotation (@ValidCategory)
 *   T = kiểu dữ liệu được validate (String)
 *
 * isValid() trả về:
 *   - true  → hợp lệ (không tạo lỗi)
 *   - false → vi phạm constraint → GlobalExceptionHandler bắt → 400
 *
 * Lưu ý: null được coi là VALID ở đây — để @NotBlank tự xử lý null riêng.
 * Đây là nguyên tắc: mỗi constraint chỉ làm đúng một việc.
 */
public class ValidCategoryValidator implements ConstraintValidator<ValidCategory, String> {

    private static final Set<String> ALLOWED = Set.of(
            "Electronics", "Books", "Clothing", "Food", "Other"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return true;           // null → do @NotBlank handle
        return ALLOWED.contains(value);
    }
}
