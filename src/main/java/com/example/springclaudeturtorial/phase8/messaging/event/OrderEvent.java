package com.example.springclaudeturtorial.phase8.messaging.event;

import java.time.Instant;

/**
 * ============================================================
 * PHASE 8 — Kafka: Event Schema
 * ============================================================
 *
 * Event là "thông điệp bất biến ghi lại điều đã xảy ra".
 * Đặt tên theo dạng: [Domain][Action]Event (quá khứ)
 *   OrderCreatedEvent, PaymentFailedEvent, UserRegisteredEvent
 *
 * Thiết kế tốt của event:
 *   ✓ Immutable (record)
 *   ✓ Chứa đủ context — consumer không cần gọi thêm API
 *   ✓ Có eventId để idempotency check
 *   ✓ Có timestamp để audit
 *   ✓ Backward-compatible khi thêm field
 *
 * Sử dụng Java Record → tự động: constructor, getters, equals, hashCode, toString
 * Jackson serialize/deserialize record từ Spring Boot 2.7+.
 * ============================================================
 */
public record OrderEvent(
        String    eventId,       // UUID — dùng để detect duplicate (idempotency)
        String    orderId,
        String    customerId,
        String    status,        // dùng String thay Enum để backward-compatible
        double    totalAmount,
        Instant   occurredAt
) {
    // Factory methods — tên rõ ràng hơn constructor
    public static OrderEvent created(String orderId, String customerId, double amount) {
        return new OrderEvent(
                java.util.UUID.randomUUID().toString(),
                orderId, customerId,
                "CREATED", amount,
                Instant.now()
        );
    }

    public static OrderEvent confirmed(String orderId, String customerId, double amount) {
        return new OrderEvent(
                java.util.UUID.randomUUID().toString(),
                orderId, customerId,
                "CONFIRMED", amount,
                Instant.now()
        );
    }

    public static OrderEvent cancelled(String orderId, String customerId, double amount) {
        return new OrderEvent(
                java.util.UUID.randomUUID().toString(),
                orderId, customerId,
                "CANCELLED", amount,
                Instant.now()
        );
    }
}
