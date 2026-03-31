package com.example.springclaudeturtorial.phase8.messaging;

import com.example.springclaudeturtorial.phase8.messaging.event.OrderEvent;
import com.example.springclaudeturtorial.phase8.messaging.producer.OrderEventProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * ============================================================
 * PHASE 8 — Kafka Controller
 * ============================================================
 *
 * HTTP endpoints để trigger Kafka events.
 * Dùng để test producer/consumer behavior thủ công.
 *
 * Mô hình Event-Driven với Kafka:
 *
 *   HTTP Request
 *       │
 *       ▼
 *   [OrderService] ──publish──► [Kafka: orders topic]
 *                                       │
 *                         ┌─────────────┼─────────────┐
 *                         ▼             ▼             ▼
 *               [InventoryService] [EmailService] [AnalyticsService]
 *                  (consumer A)      (consumer B)   (consumer C)
 *
 * Lợi ích của Event-Driven:
 *   ✓ Decoupling: OrderService không biết ai consume
 *   ✓ Scalability: thêm consumer mà không sửa producer
 *   ✓ Resilience: consumer down → message vẫn trong Kafka, xử lý khi recover
 *   ✓ Audit trail: toàn bộ lịch sử events được lưu trong Kafka
 * ============================================================
 */
@RestController
@RequestMapping("/api/v1/kafka")
public class OrderMessagingController {

    private final OrderEventProducer producer;

    public OrderMessagingController(OrderEventProducer producer) {
        this.producer = producer;
    }

    // ── POST /api/v1/kafka/orders/create ──────────────────────────────────
    @PostMapping("/orders/create")
    public ResponseEntity<Map<String, String>> createOrder(
            @RequestParam(defaultValue = "CUST-001") String customerId,
            @RequestParam(defaultValue = "50000000") double amount) {

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        OrderEvent event = OrderEvent.created(orderId, customerId, amount);

        producer.sendOrderEvent(event);

        return ResponseEntity.accepted().body(Map.of(
                "orderId",   orderId,
                "eventId",   event.eventId(),
                "status",    "CREATED",
                "message",   "Event published to Kafka"
        ));
    }

    // ── POST /api/v1/kafka/orders/{id}/confirm ────────────────────────────
    @PostMapping("/orders/{id}/confirm")
    public ResponseEntity<Map<String, String>> confirmOrder(
            @PathVariable String id,
            @RequestParam(defaultValue = "CUST-001") String customerId,
            @RequestParam(defaultValue = "50000000") double amount) {

        OrderEvent event = OrderEvent.confirmed(id, customerId, amount);
        producer.sendWithHeaders(event);   // demo gửi với headers

        return ResponseEntity.accepted().body(Map.of(
                "orderId", id,
                "status",  "CONFIRMED",
                "message", "Event published with headers"
        ));
    }

    // ── POST /api/v1/kafka/orders/{id}/cancel ─────────────────────────────
    @PostMapping("/orders/{id}/cancel")
    public ResponseEntity<Map<String, String>> cancelOrder(
            @PathVariable String id,
            @RequestParam(defaultValue = "CUST-001") String customerId) {

        OrderEvent event = OrderEvent.cancelled(id, customerId, 0);
        producer.fanOut(event);   // demo fan-out: gửi đến nhiều topic

        return ResponseEntity.accepted().body(Map.of(
                "orderId", id,
                "status",  "CANCELLED",
                "message", "Event fan-out to orders + payments topics"
        ));
    }

    // ── POST /api/v1/kafka/orders/large-payment ───────────────────────────
    // Demo @RetryableTopic: amount > 100M → consumer throw → retry → DLT
    @PostMapping("/orders/large-payment")
    public ResponseEntity<Map<String, String>> largePayment(
            @RequestParam(defaultValue = "CUST-VIP") String customerId) {

        String orderId = "ORD-LARGE-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        // amount > 100_000_000 → payment consumer sẽ throw → trigger retry chain
        OrderEvent event = OrderEvent.created(orderId, customerId, 200_000_000.0);

        // gửi vào payments topic (consumer dùng @RetryableTopic)
        producer.fanOut(event);

        return ResponseEntity.accepted().body(Map.of(
                "orderId", orderId,
                "amount",  "200,000,000",
                "message", "Will trigger retry → DLT (check logs)"
        ));
    }
}
