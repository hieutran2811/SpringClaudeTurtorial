package com.example.springclaudeturtorial.phase10.ddd.domain.event;

import java.time.Instant;

/**
 * ============================================================
 * PHASE 10 — DDD: Domain Event
 * ============================================================
 *
 * Domain Event: "sự kiện quan trọng đã xảy ra trong domain"
 *
 * Đặc điểm:
 *   - Đặt tên theo quá khứ: OrderPlaced, PaymentFailed, StockDepleted
 *   - Immutable — đã xảy ra rồi, không thể thay đổi
 *   - Chứa đủ thông tin để consumer xử lý mà không cần gọi thêm
 *
 * Domain Event vs Integration Event:
 *   Domain Event     → trong cùng Bounded Context (in-process)
 *                    → Spring @EventListener, @TransactionalEventListener
 *   Integration Event → cross-context, cross-service
 *                    → Kafka, RabbitMQ (Phase 8)
 *
 * Luồng phổ biến:
 *   1. Aggregate tạo Domain Event trong business method
 *   2. Repository/Application Layer publish event
 *   3. EventListener xử lý (có thể publish Integration Event sang Kafka)
 * ============================================================
 */
public interface DomainEvent {
    String eventId();
    Instant occurredAt();
}
