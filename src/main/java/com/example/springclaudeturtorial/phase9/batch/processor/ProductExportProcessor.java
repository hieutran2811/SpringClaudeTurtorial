package com.example.springclaudeturtorial.phase9.batch.processor;

import com.example.springclaudeturtorial.phase3.product.Product;
import com.example.springclaudeturtorial.phase9.batch.dto.ProductExportDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;

import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * ============================================================
 * PHASE 9 — Spring Batch: ItemProcessor
 * ============================================================
 *
 * ItemProcessor<I, O>:
 *   I = input type  (Product — từ DB)
 *   O = output type (ProductExportDto — ghi ra CSV)
 *
 * Vai trò:
 *   - Transform, enrich, validate từng item
 *   - Trả về null → item bị BỎ QUA (skip) — không ghi ra file
 *   - Ném exception → item fail → áp dụng skip/retry policy
 *
 * Processor chạy trong transaction của chunk.
 * Không nên làm I/O nặng ở đây (dành cho Writer).
 * ============================================================
 */
public class ProductExportProcessor implements ItemProcessor<Product, ProductExportDto> {

    private static final Logger log = LoggerFactory.getLogger(ProductExportProcessor.class);

    private static final NumberFormat VND_FORMAT =
            NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String categoryFilter;   // null = export tất cả

    public ProductExportProcessor(String categoryFilter) {
        this.categoryFilter = categoryFilter;
    }

    @Override
    public ProductExportDto process(@NonNull Product product) {
        // Trả về null → item bị skip (không ghi vào CSV)
        if (categoryFilter != null && !categoryFilter.equals(product.getCategory())) {
            log.debug("Skipping product '{}' — category '{}' not in filter '{}'",
                    product.getName(), product.getCategory(), categoryFilter);
            return null;
        }

        // Transform: enriched DTO
        String priceFormatted = VND_FORMAT.format(product.getPrice().longValue()) + " VND";
        String stockStatus    = resolveStockStatus(product.getStock());
        String exportedAt     = LocalDateTime.now().format(FORMATTER);

        log.debug("Processing product: {} → {}", product.getName(), stockStatus);

        return new ProductExportDto(
                product.getId(),
                product.getName(),
                product.getCategory(),
                priceFormatted,
                product.getStock(),
                stockStatus,
                exportedAt
        );
    }

    private String resolveStockStatus(int stock) {
        if (stock == 0)   return "OUT_OF_STOCK";
        if (stock <= 5)   return "LOW_STOCK";
        return "IN_STOCK";
    }
}
