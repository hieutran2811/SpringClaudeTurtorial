package com.example.springclaudeturtorial.phase8.messaging.consumer;

import com.example.springclaudeturtorial.phase8.messaging.config.KafkaConfig;
import com.example.springclaudeturtorial.phase8.messaging.event.OrderEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ============================================================
 * PHASE 8 — Kafka Consumer
 * ============================================================
 *
 * @KafkaListener: đánh dấu method là Kafka consumer.
 *   topics        = tên topic (hoặc pattern)
 *   groupId       = consumer group
 *   concurrency   = số thread consumer (= số partition tối đa)
 *
 * Consumer Group semantics:
 *   - Mỗi message chỉ được xử lý bởi 1 consumer trong group
 *   - Scale out: tăng số instance app → mỗi instance xử lý subset partition
 *   - Khác với pub/sub: nhiều group → mỗi group nhận đủ tất cả messages
 *
 * Acknowledgment (Manual Commit):
 *   - ack-mode: MANUAL → consumer phải gọi ack.acknowledge()
 *   - Nếu không ack → offset không được commit
 *   - Khi restart → đọc lại từ offset chưa commit
 *   - → At-least-once delivery (message có thể được xử lý nhiều lần)
 *
 * At-least-once vs At-most-once vs Exactly-once:
 *   at-most-once  : commit trước → có thể mất message nếu crash sau commit
 *   at-least-once : commit sau → có thể xử lý trùng nếu crash sau process
 *   exactly-once  : cần Kafka transactions (phức tạp, ít dùng)
 * ============================================================
 */
@Service
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    // Track đã xử lý eventId nào — idempotency guard
    // Production: dùng Redis hoặc DB thay ConcurrentHashMap
    private final ConcurrentHashMap<String, Boolean> processedEvents = new ConcurrentHashMap<>();

    // ── 1. Consumer cơ bản với Manual Acknowledgment ─────────────────────
    @KafkaListener(
            topics          = KafkaConfig.TOPIC_ORDERS,
            groupId         = "order-processor-group",
            concurrency     = "3"    // 3 thread = 3 partition → xử lý song song
    )
    public void handleOrderEvent(
            ConsumerRecord<String, OrderEvent> record,
            Acknowledgment ack) {

        OrderEvent event = record.value();

        log.info("Received [{}] orderId={} | partition={} offset={} key={}",
                event.status(),
                event.orderId(),
                record.partition(),
                record.offset(),
                record.key());

        try {
            // ── Idempotency check: đã xử lý rồi thì bỏ qua ─────────────
            if (processedEvents.putIfAbsent(event.eventId(), true) != null) {
                log.warn("Duplicate event detected, skipping: eventId={}", event.eventId());
                ack.acknowledge();   // ack dù bỏ qua — tránh redelivery vô tận
                return;
            }

            // ── Business logic theo status ────────────────────────────────
            switch (event.status()) {
                case "CREATED"   -> processOrderCreated(event);
                case "CONFIRMED" -> processOrderConfirmed(event);
                case "CANCELLED" -> processOrderCancelled(event);
                default          -> log.warn("Unknown status: {}", event.status());
            }

            // ── Commit offset sau khi xử lý thành công ───────────────────
            ack.acknowledge();

        } catch (Exception e) {
            log.error("Failed to process orderId={}: {}", event.orderId(), e.getMessage());
            // KHÔNG ack → DefaultErrorHandler sẽ retry theo config
            // Sau max retry → gửi sang DLT
            throw e;
        }
    }

    // ── 2. @RetryableTopic — retry tự động với exponential backoff ────────
    //
    // Thay vì cấu hình DefaultErrorHandler ở config class,
    // @RetryableTopic cho phép cấu hình retry per-listener.
    //
    // Tạo tự động: payments-retry-0, payments-retry-1, payments.DLT
    @RetryableTopic(
            attempts      = "4",      // 1 lần gốc + 3 retry
            backoff       = @Backoff(delay = 1000, multiplier = 2.0),
            // delay: 1s → 2s → 4s (exponential backoff)
            dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = KafkaConfig.TOPIC_PAYMENTS, groupId = "payment-processor-group")
    public void handlePaymentEvent(OrderEvent event) {
        log.info("Processing payment for orderId={} amount={}",
                event.orderId(), event.totalAmount());

        // Giả lập lỗi tạm thời để demo retry
        if (event.totalAmount() > 100_000_000) {
            throw new RuntimeException("Payment gateway timeout — will retry");
        }

        log.info("Payment processed successfully for orderId={}", event.orderId());
    }

    // ── 3. Đọc headers từ message ─────────────────────────────────────────
    @KafkaListener(topics = KafkaConfig.TOPIC_ORDERS, groupId = "audit-group")
    public void auditOrderEvent(
            ConsumerRecord<String, OrderEvent> record,
            @Header(KafkaHeaders.RECEIVED_TOPIC)     String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET)             long offset,
            Acknowledgment ack) {

        OrderEvent event = record.value();

        // Đọc custom header nếu có
        String sourceService = "unknown";
        Header srcHeader = record.headers().lastHeader("sourceService");
        if (srcHeader != null) {
            sourceService = new String(srcHeader.value(), StandardCharsets.UTF_8);
        }

        log.info("[AUDIT] topic={} partition={} offset={} source={} event={}",
                topic, partition, offset, sourceService, event.eventId());

        ack.acknowledge();
    }

    // ── 4. Consumer đọc DLT để alert/reprocess ───────────────────────────
    @KafkaListener(topics = KafkaConfig.TOPIC_ORDERS_DLT, groupId = "dlt-monitor-group")
    public void handleDeadLetterEvent(
            ConsumerRecord<String, OrderEvent> record,
            Acknowledgment ack) {

        OrderEvent event = record.value();

        // Header chứa thông tin lỗi gốc
        Header exceptionHeader = record.headers().lastHeader(
                "kafka_dlt-exception-message");
        String errorMsg = exceptionHeader != null
                ? new String(exceptionHeader.value(), StandardCharsets.UTF_8)
                : "Unknown error";

        log.error("[DLT ALERT] Message failed all retries! orderId={} error={}",
                event.orderId(), errorMsg);

        // TODO: gửi alert (Slack, PagerDuty), lưu vào DB để manual review
        ack.acknowledge();
    }

    // ── Business logic methods ────────────────────────────────────────────
    private void processOrderCreated(OrderEvent event) {
        log.info("  → Reserve inventory for order {}, amount={}",
                event.orderId(), event.totalAmount());
    }

    private void processOrderConfirmed(OrderEvent event) {
        log.info("  → Send confirmation email for order {} to customer {}",
                event.orderId(), event.customerId());
    }

    private void processOrderCancelled(OrderEvent event) {
        log.info("  → Release inventory + refund for order {}",
                event.orderId());
    }
}
