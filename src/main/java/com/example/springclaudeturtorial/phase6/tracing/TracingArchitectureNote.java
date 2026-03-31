package com.example.springclaudeturtorial.phase6.tracing;

/**
 * ============================================================
 * PHASE 6 — Kiến trúc Observability: 3 Pillars
 * ============================================================
 *
 * Observability = khả năng hiểu trạng thái bên trong hệ thống
 *                 chỉ qua dữ liệu đầu ra.
 *
 * ┌─────────────────────────────────────────────────────────────┐
 * │              3 PILLARS OF OBSERVABILITY                     │
 * ├─────────────┬─────────────────────────────────────────────┤
 * │  LOGS       │ "Chuyện gì đã xảy ra?"                      │
 * │             │ Discrete events with context                 │
 * │             │ Tool: Logback/Log4j + ELK/Loki              │
 * ├─────────────┼─────────────────────────────────────────────┤
 * │  METRICS    │ "Hệ thống đang ở trạng thái nào?"           │
 * │             │ Numeric measurements over time               │
 * │             │ Tool: Micrometer + Prometheus + Grafana      │
 * ├─────────────┼─────────────────────────────────────────────┤
 * │  TRACES     │ "Request đi qua đâu, mỗi bước mất bao lâu?"│
 * │             │ Distributed request flow                     │
 * │             │ Tool: Micrometer Tracing + Zipkin/Jaeger     │
 * └─────────────┴─────────────────────────────────────────────┘
 *
 * ── Micrometer là gì? ─────────────────────────────────────────
 *
 *   Micrometer = "SLF4J for metrics/tracing"
 *   Facade thống nhất — viết code 1 lần, export ra nhiều backend:
 *
 *   Code → MeterRegistry ─┬─► Prometheus  (pull-based, /actuator/prometheus)
 *                          ├─► Datadog     (push-based, cloud SaaS)
 *                          ├─► CloudWatch  (AWS)
 *                          └─► InfluxDB    (time-series DB)
 *
 *   Code → ObservationRegistry ─┬─► Zipkin  (trace visualization)
 *                                ├─► Jaeger  (CNCF, OpenTelemetry native)
 *                                └─► OTLP   (OpenTelemetry Protocol)
 *
 * ── Spring Boot Auto-instrumentation ─────────────────────────
 *
 *   Chỉ cần thêm dependency — Spring Boot tự instrument:
 *
 *   HTTP Inbound   → span: "http.server.requests"
 *                    metrics: http_server_requests_seconds
 *
 *   JDBC           → span: "jdbc.query"
 *                    metrics: spring.data.repository.invocations
 *
 *   WebClient      → span: "http.client.requests"
 *                    propagate traceId sang service tiếp theo
 *
 *   @Scheduled     → span: "scheduled-task"
 *
 * ── B3 Propagation (header format) ──────────────────────────
 *
 *   Khi service A gọi service B qua WebClient, Micrometer tự thêm:
 *
 *   X-B3-TraceId:       abc123  ← giữ nguyên xuyên suốt
 *   X-B3-SpanId:        def456  ← mới cho mỗi hop
 *   X-B3-ParentSpanId:  789ghi  ← spanId của service A
 *   X-B3-Sampled:       1
 *
 *   Service B nhận header → tiếp tục trace với cùng traceId.
 *
 * ── Chạy Zipkin local ────────────────────────────────────────
 *
 *   docker run -d -p 9411:9411 openzipkin/zipkin
 *
 *   Sau đó mở: http://localhost:9411
 *   Gọi: POST /api/v1/orders/process
 *   Tìm trace trong Zipkin UI theo service name "spring-tutorial"
 *
 * ── Prometheus + Grafana ─────────────────────────────────────
 *
 *   docker run -d -p 9090:9090 prom/prometheus
 *   → Cấu hình scrape: http://localhost:8080/actuator/prometheus
 *
 *   docker run -d -p 3000:3000 grafana/grafana
 *   → Import dashboard Spring Boot JVM hoặc tự tạo query
 * ============================================================
 */
class TracingArchitectureNote {
    // File này chỉ chứa documentation — không có logic
    private TracingArchitectureNote() {}
}
