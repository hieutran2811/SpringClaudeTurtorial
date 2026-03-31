package com.example.springclaudeturtorial.phase10.cqrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================
 * PHASE 10 — CQRS (Command Query Responsibility Segregation)
 * ============================================================
 *
 * CQRS = tách riêng hoàn toàn:
 *   Command (Write) side → thay đổi trạng thái, trả void/ID
 *   Query  (Read)  side  → đọc dữ liệu, không thay đổi state
 *
 * Tại sao tách?
 *
 *   Vấn đề với CRUD thông thường (cùng model):
 *     - Write cần validation, business rules, transactions
 *     - Read cần performance, denormalized data, caching
 *     - → conflict: model tối ưu cho write ≠ model tối ưu cho read
 *
 *   CQRS giải quyết bằng cách:
 *     - Write: Domain Model (rich, normalized)
 *     - Read:  Read Model (flat, denormalized, optimized for query)
 *
 * ── Levels of CQRS ────────────────────────────────────────
 *
 *   Level 1 (Simple): Tách method trong cùng class
 *                     → ít lợi ích, nhưng tư duy rõ ràng
 *
 *   Level 2 (Medium): Tách thành 2 service riêng biệt
 *                     CommandService + QueryService          ← file này
 *                     → dễ scale, test độc lập
 *
 *   Level 3 (Full):   Tách DB riêng
 *                     Write DB: normalized (PostgreSQL, MySQL)
 *                     Read DB:  denormalized (Elasticsearch, Redis, MongoDB)
 *                     Sync qua events (Kafka)
 *                     → complex, chỉ khi thực sự cần scale
 *
 * ── Khi nào dùng CQRS? ───────────────────────────────────
 *   ✓ Read và Write có performance requirements khác nhau
 *   ✓ Read pattern phức tạp (nhiều join, aggregation)
 *   ✓ Cần scale read riêng (đọc nhiều hơn ghi gấp 10x)
 *   ✗ CRUD đơn giản — CQRS là over-engineering
 *   ✗ Team nhỏ, deadline gấp
 *
 * ── Event Sourcing (nâng cao hơn CQRS) ──────────────────
 *   Thay vì lưu current state → lưu chuỗi events
 *   State = replay tất cả events
 *   → Full audit trail, time travel debugging
 *   → Phức tạp hơn nhiều, không phổ biến
 * ============================================================
 */
@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    private static final Logger log = LoggerFactory.getLogger(OrderQueryService.class);

    // Trong production: đây là READ DB riêng (Elasticsearch, Redis, denormalized table)
    // Ở đây dùng Map để demo concept
    private final Map<String, OrderReadModel> readStore = new ConcurrentHashMap<>();

    // ── READ SIDE: trả về Read Model (denormalized, optimized) ───────────
    //
    // Read Model khác Domain Model:
    //   - Flat (không cần traverse relations)
    //   - Có thể chứa computed fields (totalFormatted)
    //   - Có thể được cache aggressively
    //   - Không có business methods
    public OrderReadModel findById(String orderId) {
        log.info("[QUERY] findById: {}", orderId);
        return readStore.getOrDefault(orderId,
                new OrderReadModel(orderId, "UNKNOWN", "N/A", 0, "N/A"));
    }

    public List<OrderReadModel> findByCustomer(String customerId) {
        log.info("[QUERY] findByCustomer: {}", customerId);
        return readStore.values().stream()
                .filter(o -> customerId.equals(o.customerId()))
                .toList();
    }

    public List<OrderReadModel> findByStatus(String status) {
        log.info("[QUERY] findByStatus: {}", status);
        return readStore.values().stream()
                .filter(o -> status.equals(o.status()))
                .toList();
    }

    // ── Projection: cập nhật Read Model khi có Domain Event ──────────────
    //
    // @EventListener lắng nghe Domain Event từ Write Side
    // → rebuild/update Read Model
    // Đây là cách Level 2 CQRS đồng bộ dữ liệu giữa 2 side.
    @org.springframework.context.event.EventListener
    @Transactional
    public void on(com.example.springclaudeturtorial.phase10.ddd.domain.event.OrderPlacedEvent event) {
        log.info("[CQRS Projection] Updating read model for order={}", event.orderId());

        var readModel = new OrderReadModel(
                event.orderId().value(),
                event.customerId(),
                "PLACED",
                event.itemCount(),
                event.totalAmount().toString()
        );
        readStore.put(event.orderId().value(), readModel);
    }

    // ── Read Model (denormalized DTO) ─────────────────────────────────────
    public record OrderReadModel(
            String orderId,
            String customerId,
            String status,
            int    itemCount,
            String totalFormatted     // "25,000,000 VND" — precomputed
    ) {}
}
