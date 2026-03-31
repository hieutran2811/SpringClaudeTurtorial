package com.example.springclaudeturtorial.phase8.messaging.producer;

import com.example.springclaudeturtorial.phase8.messaging.config.KafkaConfig;
import com.example.springclaudeturtorial.phase8.messaging.event.OrderEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ============================================================
 * PHASE 8 — Kafka Producer
 * ============================================================
 *
 * KafkaTemplate: Spring abstraction để gửi message.
 *   send(topic, value)              → random partition
 *   send(topic, key, value)         → partition theo key hash
 *   send(topic, partition, key, value) → partition chỉ định
 *
 * Key cho Partition Routing:
 *   - Message cùng key luôn vào cùng partition
 *   - → Ordering guarantee cho cùng key
 *   - Dùng orderId làm key → tất cả events của 1 order theo đúng thứ tự
 *
 * Async vs Sync:
 *   - send() trả CompletableFuture → non-blocking (async)
 *   - .get() chờ kết quả → blocking (chỉ dùng khi thật sự cần)
 *   - Production: dùng async + callback để xử lý success/failure
 * ============================================================
 */
@Service
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // ── 1. Gửi async với callback ─────────────────────────────────────────
    public void sendOrderEvent(OrderEvent event) {
        CompletableFuture<SendResult<String, OrderEvent>> future =
                kafkaTemplate.send(
                        KafkaConfig.TOPIC_ORDERS,
                        event.orderId(),     // key → routing đến cùng partition
                        event
                );

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                var metadata = result.getRecordMetadata();
                log.info("Sent [{}] orderId={} → topic={} partition={} offset={}",
                        event.status(),
                        event.orderId(),
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset());
            } else {
                log.error("Failed to send event orderId={}: {}",
                        event.orderId(), ex.getMessage());
            }
        });
    }

    // ── 2. Gửi với custom Headers ─────────────────────────────────────────
    //
    // Headers = metadata đi kèm message, không phải payload
    // Dùng để: routing, tracing, versioning, source service ID
    public void sendWithHeaders(OrderEvent event) {
        var record = new ProducerRecord<>(
                KafkaConfig.TOPIC_ORDERS,
                null,               // partition null → auto-assign by key
                event.orderId(),    // key
                event,
                List.of(
                    new RecordHeader("eventVersion",
                            "v1".getBytes(StandardCharsets.UTF_8)),
                    new RecordHeader("sourceService",
                            "spring-tutorial".getBytes(StandardCharsets.UTF_8)),
                    new RecordHeader("correlationId",
                            event.eventId().getBytes(StandardCharsets.UTF_8))
                )
        );

        kafkaTemplate.send(record).whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent with headers: orderId={} eventId={}",
                        event.orderId(), event.eventId());
            } else {
                log.error("Send with headers failed: {}", ex.getMessage());
            }
        });
    }

    // ── 3. Gửi đến nhiều topic (fan-out pattern) ──────────────────────────
    //
    // Fan-out: 1 event → nhiều topic → nhiều consumer độc lập xử lý
    // Ví dụ: OrderCreated → [inventory-service, email-service, analytics]
    public void fanOut(OrderEvent event) {
        log.info("Fan-out event orderId={} to multiple topics", event.orderId());

        // Gửi đến orders topic (inventory-service lắng nghe)
        kafkaTemplate.send(KafkaConfig.TOPIC_ORDERS, event.orderId(), event);

        // Gửi đến payments topic (payment-service lắng nghe)
        kafkaTemplate.send(KafkaConfig.TOPIC_PAYMENTS, event.orderId(), event);
    }
}
