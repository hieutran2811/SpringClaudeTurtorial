package com.example.springclaudeturtorial.phase8;

import com.example.springclaudeturtorial.phase8.messaging.config.KafkaConfig;
import com.example.springclaudeturtorial.phase8.messaging.event.OrderEvent;
import com.example.springclaudeturtorial.phase8.messaging.producer.OrderEventProducer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;

/**
 * ============================================================
 * PHASE 8 — Kafka Integration Test với @EmbeddedKafka
 * ============================================================
 *
 * @EmbeddedKafka: chạy Kafka broker ngay trong JVM — không cần Docker.
 *   Nhanh hơn Testcontainers cho Kafka test.
 *   Phù hợp cho: unit/integration test của producer + consumer logic.
 *
 * Testcontainers Kafka (org.testcontainers:kafka):
 *   Chạy Kafka thật trong Docker.
 *   Phù hợp hơn khi cần test behavior chính xác 100% giống production.
 *
 * @DirtiesContext: sau test reset ApplicationContext
 *   → tránh embedded Kafka state ảnh hưởng test khác
 * ============================================================
 */
@SpringBootTest
@ActiveProfiles("local")
@EmbeddedKafka(
        partitions       = 3,
        brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
        topics           = {KafkaConfig.TOPIC_ORDERS, KafkaConfig.TOPIC_PAYMENTS,
                            KafkaConfig.TOPIC_ORDERS_DLT}
)
@DirtiesContext
@DisplayName("Kafka Messaging — Integration Tests")
class KafkaMessagingIntegrationTest {

    @Autowired
    OrderEventProducer producer;

    // ── Test 1: Producer gửi thành công ───────────────────────────────────
    @Test
    @DisplayName("Producer gửi OrderEvent thành công")
    void producer_sendsEvent_successfully() {
        OrderEvent event = OrderEvent.created("ORD-TEST-001", "CUST-001", 50_000_000.0);

        // Gửi event — không throw exception là thành công
        assertThatCode(() -> producer.sendOrderEvent(event))
                .doesNotThrowAnyException();
    }

    // ── Test 2: Fan-out gửi đến nhiều topic ───────────────────────────────
    @Test
    @DisplayName("Fan-out producer gửi đến orders + payments topics")
    void producer_fanOut_sendsToMultipleTopics() {
        OrderEvent event = OrderEvent.cancelled("ORD-TEST-002", "CUST-002", 30_000_000.0);

        assertThatCode(() -> producer.fanOut(event))
                .doesNotThrowAnyException();
    }

    // ── Test 3: Consumer xử lý message (cần CountDownLatch) ───────────────
    //
    // Consumer chạy async → cần đồng bộ hóa với test thread.
    // Pattern phổ biến: inject CountDownLatch vào consumer, await trong test.
    //
    // Simplified version: chỉ verify producer không fail
    // Full version: dùng @SpyBean + ArgumentCaptor hoặc shared CountDownLatch
    @Test
    @DisplayName("Event với headers được gửi thành công")
    void producer_sendWithHeaders_successfully() throws InterruptedException {
        OrderEvent event = OrderEvent.confirmed("ORD-TEST-003", "CUST-003", 75_000_000.0);

        producer.sendWithHeaders(event);

        // Chờ ngắn để async callback hoàn thành
        TimeUnit.MILLISECONDS.sleep(500);

        // Nếu không có exception → test pass
        // Full test: kiểm tra consumer đã nhận bằng CountDownLatch
    }
}
