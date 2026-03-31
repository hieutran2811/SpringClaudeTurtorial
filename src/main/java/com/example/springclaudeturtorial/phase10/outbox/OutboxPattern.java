package com.example.springclaudeturtorial.phase10.outbox;

import jakarta.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * ============================================================
 * PHASE 10 — Outbox Pattern (Reliable Messaging)
 * ============================================================
 *
 * Vấn đề "Dual Write":
 *
 *   orderRepo.save(order);           // ← DB write
 *   kafkaTemplate.send("orders", e); // ← Kafka write
 *
 *   Nếu DB commit thành công nhưng Kafka fail:
 *   → Order trong DB nhưng event không được publish
 *   → Inventory service không biết để reserve
 *   → Data inconsistency!
 *
 *   Nếu Kafka thành công nhưng DB rollback:
 *   → Event đã gửi nhưng order không tồn tại
 *
 * ── Outbox Pattern giải quyết ─────────────────────────────
 *
 *   Thay vì gửi Kafka trực tiếp, ghi vào bảng OUTBOX
 *   cùng transaction với domain data:
 *
 *   BEGIN TRANSACTION
 *     INSERT INTO orders (...)           ← domain data
 *     INSERT INTO outbox_events (...)    ← event record
 *   COMMIT
 *
 *   → Nếu transaction fail → cả 2 rollback → consistent
 *   → Nếu thành công → background job đọc outbox → publish Kafka
 *
 *   Relay (background job):
 *     Loop: SELECT unpublished FROM outbox
 *           → publish to Kafka
 *           → mark as published
 *
 * ── Guarantees ────────────────────────────────────────────
 *   At-least-once: message có thể publish 2 lần nếu relay crash
 *   → Consumer cần idempotency (eventId check — Phase 8)
 *
 * ── Implementations ──────────────────────────────────────
 *   Simple: polling @Scheduled (file này)
 *   Better: Debezium CDC (Change Data Capture) — đọc DB transaction log
 *           → zero-latency, không cần polling
 * ============================================================
 */
public class OutboxPattern {

    // ── Entity: OutboxEvent ───────────────────────────────────────────────
    @Entity
    @Table(name = "outbox_events")
    public static class OutboxEvent {

        @Id
        private String id;

        @Column(nullable = false)
        private String aggregateType;   // "Order", "Product"

        @Column(nullable = false)
        private String aggregateId;

        @Column(nullable = false)
        private String eventType;       // "OrderPlaced", "OrderCancelled"

        @Column(columnDefinition = "TEXT", nullable = false)
        private String payload;         // JSON của event

        @Column(nullable = false)
        private Instant createdAt;

        private Instant publishedAt;    // null = chưa publish

        protected OutboxEvent() {}

        public OutboxEvent(String aggregateType, String aggregateId,
                           String eventType, String payload) {
            this.id            = UUID.randomUUID().toString();
            this.aggregateType = aggregateType;
            this.aggregateId   = aggregateId;
            this.eventType     = eventType;
            this.payload       = payload;
            this.createdAt     = Instant.now();
        }

        public boolean isPublished()             { return publishedAt != null; }
        public void    markPublished()           { this.publishedAt = Instant.now(); }
        public String  getId()                   { return id; }
        public String  getEventType()            { return eventType; }
        public String  getAggregateId()          { return aggregateId; }
        public String  getPayload()              { return payload; }
        public Instant getPublishedAt()          { return publishedAt; }
    }

    // ── Repository ────────────────────────────────────────────────────────
    public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
        List<OutboxEvent> findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();
    }

    // ── Outbox Writer: ghi event vào outbox trong cùng transaction ────────
    @Service
    public static class OutboxWriter {

        private final OutboxEventRepository outboxRepo;

        public OutboxWriter(OutboxEventRepository outboxRepo) {
            this.outboxRepo = outboxRepo;
        }

        // @TransactionalEventListener(phase = BEFORE_COMMIT):
        //   chạy trong transaction của caller, TRƯỚC khi commit
        //   → outbox record ghi cùng transaction với domain data
        @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
        public void onOrderPlaced(
                com.example.springclaudeturtorial.phase10.ddd.domain.event.OrderPlacedEvent event) {

            String payload = """
                    {"orderId":"%s","customerId":"%s","total":"%s","items":%d}
                    """.formatted(
                    event.orderId().value(),
                    event.customerId(),
                    event.totalAmount().toString(),
                    event.itemCount()
            );

            outboxRepo.save(new OutboxEvent(
                    "Order",
                    event.orderId().value(),
                    "OrderPlaced",
                    payload.strip()
            ));

            LoggerFactory.getLogger(OutboxWriter.class)
                    .info("[Outbox] Saved OutboxEvent for order={}", event.orderId());
        }
    }

    // ── Relay: đọc outbox và publish sang Kafka ───────────────────────────
    @Component
    public static class OutboxRelay {

        private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

        private final OutboxEventRepository outboxRepo;

        public OutboxRelay(OutboxEventRepository outboxRepo) {
            this.outboxRepo = outboxRepo;
        }

        // Polling mỗi 5 giây — production: dùng Debezium CDC thay vì polling
        @Scheduled(fixedDelay = 5_000)
        @Transactional
        public void publishPendingEvents() {
            List<OutboxEvent> pending =
                    outboxRepo.findTop100ByPublishedAtIsNullOrderByCreatedAtAsc();

            if (pending.isEmpty()) return;

            log.info("[Outbox Relay] Processing {} pending events", pending.size());

            for (OutboxEvent outboxEvent : pending) {
                try {
                    // TODO: inject KafkaTemplate và publish thực sự
                    // kafkaTemplate.send("orders", outboxEvent.getAggregateId(),
                    //                    outboxEvent.getPayload());

                    log.info("[Outbox Relay] Published: type={} aggregateId={}",
                            outboxEvent.getEventType(),
                            outboxEvent.getAggregateId());

                    outboxEvent.markPublished();  // mark trong cùng transaction
                    outboxRepo.save(outboxEvent);

                } catch (Exception e) {
                    log.error("[Outbox Relay] Failed to publish event {}: {}",
                            outboxEvent.getId(), e.getMessage());
                    // Không throw — tiếp tục xử lý event khác
                    // Event này sẽ được retry lần sau
                }
            }
        }
    }
}
