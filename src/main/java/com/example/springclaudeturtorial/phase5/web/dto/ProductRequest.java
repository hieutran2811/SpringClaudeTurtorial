package com.example.springclaudeturtorial.phase5.web.dto;

import com.example.springclaudeturtorial.phase5.web.validation.ValidCategory;
import jakarta.validation.constraints.*;

/**
 * ============================================================
 * PHASE 5 — Request DTO + Validation Groups
 * ============================================================
 *
 * Tại sao dùng DTO thay vì validate trực tiếp trên Entity?
 *
 *   Entity (@Entity) = domain model → ánh xạ với database
 *   DTO (Record)     = API contract → những gì client được phép gửi
 *
 * Tách biệt này giúp:
 *   1. Không expose internal field của entity (vd: id, version, createdAt)
 *   2. Validation rules cho API có thể khác rule của DB
 *   3. Create vs Update có thể có rule khác nhau → dùng Validation Groups
 *
 * ── Validation Groups ─────────────────────────────────────
 * Groups = interface rỗng, chỉ dùng làm "nhãn" để nhóm constraints.
 *
 *   @NotBlank(groups = OnCreate.class) → chỉ validate khi CREATE
 *   @Null(groups = OnCreate.class)     → field này phải null khi CREATE
 *
 * Controller dùng @Validated(OnCreate.class) hoặc @Validated(OnUpdate.class)
 * thay vì @Valid để kích hoạt đúng group.
 * ============================================================
 */
public record ProductRequest(

        // id: client KHÔNG được gửi khi tạo mới, nhưng có thể gửi khi update
        @Null(groups = OnCreate.class, message = "ID must not be provided when creating")
        Long id,

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Category is required")
        @ValidCategory                           // ← custom constraint
        String category,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Price must be positive")
        @DecimalMax(value = "999999.99", message = "Price must not exceed 999,999.99")
        Double price,

        @Min(value = 0, message = "Stock cannot be negative")
        @Max(value = 100_000, message = "Stock cannot exceed 100,000")
        Integer stock
) {
    // ── Validation Group interfaces ────────────────────────────────────────
    public interface OnCreate {}   // nhãn cho POST
    public interface OnUpdate {}   // nhãn cho PUT
}
