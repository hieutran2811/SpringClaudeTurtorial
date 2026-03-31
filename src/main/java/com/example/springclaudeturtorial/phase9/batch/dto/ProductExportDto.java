package com.example.springclaudeturtorial.phase9.batch.dto;

/**
 * ============================================================
 * PHASE 9 — Spring Batch: Output DTO
 * ============================================================
 *
 * DTO riêng cho Batch export — khác với API DTO.
 * Lý do tách: format CSV khác format JSON,
 *   cần thêm field như exportedAt, priceFormatted.
 * ============================================================
 */
public record ProductExportDto(
        Long   id,
        String name,
        String category,
        String priceFormatted,   // "25,000,000 VND"
        int    stock,
        String stockStatus,      // "IN_STOCK" / "LOW_STOCK" / "OUT_OF_STOCK"
        String exportedAt
) {}
