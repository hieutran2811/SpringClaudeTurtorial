package com.example.springclaudeturtorial.phase6.tracing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * ============================================================
 * PHASE 6 — Tracing Controller + Custom Metrics
 * ============================================================
 *
 * Mỗi HTTP request vào đây được tự động tạo span bởi Spring MVC.
 * Các method gọi OrderService sẽ tạo child spans.
 *
 * Kết quả trong Zipkin UI:
 *
 *   [GET /api/v1/orders/{id}/process]        ← root span (HTTP)
 *     └─ [order.process]                     ← @NewSpan
 *           ├─ [order.validate]              ← @NewSpan
 *           ├─ [order.inventory]             ← @NewSpan
 *           └─ [order.payment]               ← @NewSpan
 *
 * Bonus: Custom Metrics với MeterRegistry
 *   Micrometer cung cấp facade thống nhất cho:
 *   Prometheus, Datadog, CloudWatch, InfluxDB, ...
 *   Chỉ cần đổi dependency — code không đổi.
 * ============================================================
 */
@RestController
@RequestMapping("/api/v1/orders")
public class TracingController {

    private static final Logger log = LoggerFactory.getLogger(TracingController.class);

    private final OrderService  orderService;

    // ── Custom Metrics với MeterRegistry ──────────────────────────────────
    private final Counter ordersProcessedCounter;
    private final Counter ordersFailedCounter;
    private final Timer   orderProcessingTimer;

    public TracingController(OrderService orderService, MeterRegistry meterRegistry) {
        this.orderService = orderService;

        // Counter: chỉ tăng — đếm số lần xảy ra sự kiện
        this.ordersProcessedCounter = Counter.builder("orders.processed")
                .description("Total number of successfully processed orders")
                .tag("service", "order")
                .register(meterRegistry);

        this.ordersFailedCounter = Counter.builder("orders.failed")
                .description("Total number of failed orders")
                .tag("service", "order")
                .register(meterRegistry);

        // Timer: đo thời gian + count + histogram
        // Tự động tạo: orders_processing_seconds_count, _sum, _max, _bucket
        this.orderProcessingTimer = Timer.builder("orders.processing.duration")
                .description("Time taken to process an order")
                .tag("service", "order")
                .publishPercentiles(0.5, 0.95, 0.99)    // p50, p95, p99
                .register(meterRegistry);
    }

    // ── POST /api/v1/orders/process ───────────────────────────────────────
    // Tạo trace với nhiều child spans — xem trong Zipkin
    @PostMapping("/process")
    public ResponseEntity<Map<String, Object>> processOrder(
            @RequestParam(defaultValue = "") String customerId) {

        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        log.info("Received process order request — orderId={}, customerId={}", orderId, customerId);

        try {
            // Timer.record(): đo thời gian + ghi metric
            String result = orderProcessingTimer.record(() ->
                    orderService.processOrder(orderId, customerId));

            ordersProcessedCounter.increment();

            return ResponseEntity.ok(Map.of(
                    "orderId",   orderId,
                    "result",    result,
                    "status",    "SUCCESS"
            ));
        } catch (Exception e) {
            ordersFailedCounter.increment();
            log.error("Order processing failed for {}", orderId, e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "orderId", orderId,
                    "error",   e.getMessage()
            ));
        }
    }

    // ── GET /api/v1/orders/{id}/status ────────────────────────────────────
    // Trace ID sẽ xuất hiện trong response — dùng để tra cứu trong Zipkin
    @GetMapping("/{id}/status")
    public ResponseEntity<Map<String, String>> getStatus(@PathVariable String id) {
        String status = orderService.getOrderStatus(id);
        return ResponseEntity.ok(Map.of("orderId", id, "status", status));
    }

    // ── POST /api/v1/orders/{id}/refund ──────────────────────────────────
    // Demo Observation API (đo cả metrics + trace)
    @PostMapping("/{id}/refund")
    public ResponseEntity<Map<String, String>> refund(
            @PathVariable String id,
            @RequestParam(defaultValue = "customer_request") String reason) {

        String result = orderService.processRefund(id, reason);
        return ResponseEntity.ok(Map.of("result", result));
    }

    // ── GET /api/v1/orders/metrics ────────────────────────────────────────
    // Xem custom metrics hiện tại (cũng có ở /actuator/prometheus)
    @GetMapping("/metrics")
    public ResponseEntity<Map<String, Object>> getMetrics() {
        return ResponseEntity.ok(Map.of(
                "ordersProcessed",  ordersProcessedCounter.count(),
                "ordersFailed",     ordersFailedCounter.count(),
                "avgProcessingMs",  orderProcessingTimer.mean(java.util.concurrent.TimeUnit.MILLISECONDS),
                "maxProcessingMs",  orderProcessingTimer.max(java.util.concurrent.TimeUnit.MILLISECONDS),
                "tip", "Full metrics at /actuator/prometheus"
        ));
    }
}
