package com.example.springclaudeturtorial.phase4.jpa.projection;

import java.math.BigDecimal;

/**
 * Projection Interface — chỉ lấy đúng field cần thiết
 *
 * Spring Data tự tạo proxy implement interface này.
 * Query chỉ SELECT id, name, price — KHÔNG load stock, category, tags, version.
 *
 * Dùng khi: API trả về danh sách cần light-weight response.
 */
public interface ItemSummary {
    Long       getId();
    String     getName();
    BigDecimal getPrice();

    // Derived method trong projection — Spring tự tính
    default String getDisplayPrice() {
        return String.format("%,.0f đ", getPrice());
    }
}
