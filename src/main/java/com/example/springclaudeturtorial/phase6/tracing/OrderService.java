package com.example.springclaudeturtorial.phase6.tracing;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.annotation.NewSpan;
import io.micrometer.tracing.annotation.SpanTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * ============================================================
 * PHASE 6 — Distributed Tracing với Micrometer
 * ============================================================
 *
 * Distributed Tracing giải quyết câu hỏi:
 *   "Request này đi qua những service nào, mỗi bước mất bao lâu?"
 *
 * Khái niệm cốt lõi:
 *
 *   Trace   = toàn bộ hành trình của 1 request (xuyên nhiều service)
 *             Được định danh bằng traceId (16 hex chars)
 *
 *   Span    = 1 đơn vị công việc trong trace (vd: 1 DB query, 1 HTTP call)
 *             Có: spanId, parentSpanId, startTime, endTime, tags, events
 *
 *   Context = traceId + spanId truyền qua HTTP headers giữa các service:
 *             X-B3-TraceId, X-B3-SpanId, X-B3-ParentSpanId (B3 format)
 *
 * ── Những gì Spring Boot tự động trace (không cần code thêm) ──
 *   ✓ HTTP requests (MVC + WebFlux)
 *   ✓ JDBC queries (Spring Data)
 *   ✓ WebClient outbound calls
 *   ✓ @Scheduled tasks
 *   ✓ Kafka/RabbitMQ messages
 *
 * ── Khi nào cần custom span? ──────────────────────────────────
 *   - Logic business quan trọng cần đo thời gian
 *   - External calls không được auto-instrument
 *   - Thêm context (tag, event) vào span cho debugging
 * ============================================================
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    // ObservationRegistry: API trung tâm của Micrometer
    // Ghi cả metrics VÀ traces cùng lúc qua một điểm
    private final ObservationRegistry observationRegistry;

    public OrderService(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    // ── 1. @NewSpan — cách đơn giản nhất ─────────────────────────────────
    //
    // @NewSpan: Spring AOP tự tạo span mới khi method được gọi,
    //           kết thúc span khi method return (hoặc throw).
    //           Span này là CON của span hiện tại trong thread (nếu có).
    //
    // @SpanTag: gán key-value metadata vào span
    //           Dùng để filter/search trace trong Zipkin UI
    @NewSpan("order.process")
    public String processOrder(
            @SpanTag("order.id")       String orderId,
            @SpanTag("order.customer") String customerId) {

        log.info("Processing order {} for customer {}", orderId, customerId);

        // Gọi các bước — mỗi @NewSpan method sẽ tạo child span
        validateOrder(orderId);
        reserveInventory(orderId);
        chargePayment(orderId, 150_000.0);

        log.info("Order {} processed successfully", orderId);
        return "Order " + orderId + " confirmed";
    }

    @NewSpan("order.validate")
    public void validateOrder(@SpanTag("order.id") String orderId) {
        log.info("  Validating order {}", orderId);
        simulateWork(20);   // giả lập 20ms processing
    }

    @NewSpan("order.inventory")
    public void reserveInventory(@SpanTag("order.id") String orderId) {
        log.info("  Reserving inventory for order {}", orderId);
        simulateWork(50);
    }

    @NewSpan("order.payment")
    public void chargePayment(
            @SpanTag("order.id") String orderId,
            @SpanTag("payment.amount") double amount) {
        log.info("  Charging payment {} for order {}", amount, orderId);
        simulateWork(80);
    }

    // ── 2. Observation API — kiểm soát chi tiết hơn ───────────────────────
    //
    // Observation đo cả metrics + traces:
    //   - Tạo span trong trace
    //   - Tạo timer metric (histogram) tự động
    //
    // observation.name() → tên metric + tên span
    // lowCardinalityKeyValue() → tag có ít giá trị khác nhau (index tốt)
    // highCardinalityKeyValue() → tag có nhiều giá trị (vd: user ID — KHÔNG index)
    public String processRefund(String orderId, String reason) {
        return Observation.createNotStarted("order.refund", observationRegistry)
                .lowCardinalityKeyValue("reason", reason)       // vd: "duplicate", "fraud"
                .highCardinalityKeyValue("order.id", orderId)   // unique per order
                .observe(() -> {
                    log.info("Processing refund for order {} — reason: {}", orderId, reason);
                    simulateWork(60);

                    // event: ghi nhận điểm quan trọng trong span (timestamp + name)
                    // Hiện thị trong Zipkin như annotation
                    Observation.CheckedCallable<String, RuntimeException> step = () -> {
                        log.info("  Refund approved");
                        return "Refund for " + orderId + " processed";
                    };

                    try { return step.call(); }
                    catch (RuntimeException e) { throw e; }
                });
    }

    // ── 3. Lấy traceId từ MDC để log tương quan ───────────────────────────
    //
    // Micrometer tự động đặt traceId, spanId vào MDC (Mapped Diagnostic Context)
    // → Logback/Log4j tự in vào mọi log line của request đó
    // → Có thể grep log theo traceId để xem toàn bộ request
    //
    // Log output sẽ trông như:
    //   INFO [spring-tutorial,65f3a1b2c4d5e6f7,a1b2c3d4e5f60001] OrderService - Processing order...
    //                          └─ traceId ────────┘└─ spanId ──────┘
    public String getOrderStatus(String orderId) {
        String traceId = org.slf4j.MDC.get("traceId");
        log.info("Getting status for order {} (traceId: {})", orderId, traceId);
        simulateWork(10);
        return "Order " + orderId + " status: CONFIRMED [trace=" + traceId + "]";
    }

    private void simulateWork(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
