package com.example.springclaudeturtorial.phase8.messaging.config;

import com.example.springclaudeturtorial.phase8.messaging.event.OrderEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * ============================================================
 * PHASE 8 — Kafka Configuration
 * ============================================================
 *
 * ── Kafka Concepts ────────────────────────────────────────
 *
 *   Topic      = category/feed of messages (như một queue có nhiều tính năng)
 *   Partition  = đơn vị song song trong topic
 *                → nhiều partition = nhiều consumer đọc song song
 *                → message cùng key luôn vào cùng partition (ordering guarantee)
 *   Offset     = vị trí của message trong partition (số nguyên tăng dần)
 *   Consumer Group = nhóm consumer cùng đọc một topic
 *                → mỗi partition chỉ được đọc bởi 1 consumer trong group
 *                → scale out: thêm consumer vào group
 *
 *   Producer ──► [Topic: orders]
 *                   ├── Partition 0: [msg0] [msg3] [msg6]
 *                   ├── Partition 1: [msg1] [msg4] [msg7]
 *                   └── Partition 2: [msg2] [msg5] [msg8]
 *                          ▼
 *                [Consumer Group: spring-tutorial-group]
 *                   ├── Consumer A → Partition 0
 *                   ├── Consumer B → Partition 1
 *                   └── Consumer C → Partition 2
 *
 * ── Dead Letter Topic (DLT) ──────────────────────────────
 *   Khi consumer fail sau N lần retry → message chuyển sang DLT
 *   DLT name convention: {original-topic}.DLT
 *   → Không mất message, có thể xử lý lại sau
 * ============================================================
 */
@Configuration
public class KafkaConfig {

    public static final String TOPIC_ORDERS     = "orders";
    public static final String TOPIC_ORDERS_DLT = "orders.DLT";
    public static final String TOPIC_PAYMENTS   = "payments";

    // ── Topic definitions ─────────────────────────────────────────────────
    // Spring Boot tự tạo topic nếu chưa có (chỉ khi auto-create được phép)
    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(TOPIC_ORDERS)
                .partitions(3)      // 3 partition → 3 consumer song song tối đa
                .replicas(1)        // 1 replica (local dev); production: ≥2
                .build();
    }

    @Bean
    public NewTopic ordersDltTopic() {
        return TopicBuilder.name(TOPIC_ORDERS_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentsTopic() {
        return TopicBuilder.name(TOPIC_PAYMENTS)
                .partitions(3)
                .replicas(1)
                .build();
    }

    // ── Error Handler với Dead Letter Topic ───────────────────────────────
    //
    // DefaultErrorHandler: xử lý exception trong @KafkaListener
    //   - Retry N lần với interval cố định
    //   - Sau đó publish message lỗi sang DLT
    //
    // DeadLetterPublishingRecoverer: ghi message failed + exception vào DLT
    //   - Header DLT chứa: original topic, partition, offset, exception
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        // Retry tối đa 3 lần, mỗi lần cách nhau 1 giây
        var backOff = new FixedBackOff(1_000L, 3L);

        // Sau 3 lần fail → publish sang DLT
        var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
