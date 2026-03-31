package com.example.springclaudeturtorial.phase10.hexagonal;

import com.example.springclaudeturtorial.phase10.ddd.domain.aggregate.OrderAggregate;
import com.example.springclaudeturtorial.phase10.ddd.domain.event.DomainEvent;
import com.example.springclaudeturtorial.phase10.ddd.domain.valueobject.Money;
import com.example.springclaudeturtorial.phase10.ddd.domain.valueobject.OrderId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ============================================================
 * PHASE 10 — Hexagonal Architecture (Ports & Adapters)
 * ============================================================
 *
 * Hexagonal Architecture = "Domain ở trung tâm, không phụ thuộc gì"
 *
 *         ┌─────────────────────────────────────┐
 *         │           ADAPTERS (in)              │
 *         │   REST Controller, Kafka Consumer    │
 *         └──────────────┬──────────────────────┘
 *                        │ calls
 *         ┌──────────────▼──────────────────────┐
 *         │        APPLICATION LAYER             │
 *         │   Use Cases / Application Services   │ ← file này
 *         │   (orchestrate domain + ports)        │
 *         └──────────────┬──────────────────────┘
 *                        │ uses
 *         ┌──────────────▼──────────────────────┐
 *         │           DOMAIN LAYER               │
 *         │   Aggregate, Value Object, Event     │ ← phase10/ddd/domain
 *         └──────────────────────────────────────┘
 *                        │ implemented by
 *         ┌──────────────▼──────────────────────┐
 *         │        ADAPTERS (out)                │
 *         │   JPA Repository, Kafka Producer     │
 *         └─────────────────────────────────────┘
 *
 * ── Ports ─────────────────────────────────────────────────
 *   Inbound Port  = Use Case interface (ví dụ: PlaceOrderUseCase)
 *   Outbound Port = Repository/messaging interface (ví dụ: OrderRepository)
 *
 * ── Dependency Rule ───────────────────────────────────────
 *   Infrastructure → Application → Domain
 *   Domain KHÔNG import gì từ Spring, JPA, Kafka
 *   → Domain thuần Java → dễ test, dễ migrate framework
 *
 * ── So sánh với Layered Architecture ─────────────────────
 *   Layered:    Controller → Service → Repository → DB
 *               (phụ thuộc 1 chiều từ trên xuống)
 *               VẤN ĐỀ: Service phụ thuộc vào JPA Repository
 *               → khó test, khó swap DB
 *
 *   Hexagonal:  Service phụ thuộc vào INTERFACE (port)
 *               JPA Repo implements port → Dependency Inversion
 *               → dễ swap (H2 test, MongoDB production)
 * ============================================================
 */
@Service
@Transactional
public class PlaceOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(PlaceOrderUseCase.class);

    // ── Outbound Ports (interfaces — không phụ thuộc implementation) ──────
    private final OrderRepositoryPort     orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    // Constructor injection — tường minh về dependencies
    public PlaceOrderUseCase(OrderRepositoryPort orderRepository,
                             ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher  = eventPublisher;
    }

    /**
     * Use Case: Đặt đơn hàng.
     *
     * Application Layer orchestrate:
     * 1. Tạo Aggregate
     * 2. Gọi domain business methods
     * 3. Persist qua port
     * 4. Publish domain events
     *
     * KHÔNG chứa business logic — đó là việc của Aggregate.
     */
    public OrderId execute(PlaceOrderCommand command) {
        log.info("Use case: PlaceOrder for customer={}", command.customerId());

        // 1. Tạo aggregate (factory method)
        OrderAggregate order = OrderAggregate.create(command.customerId());

        // 2. Gọi domain methods — business logic ở trong aggregate
        command.items().forEach(item ->
                order.addItem(
                        item.productId(),
                        item.productName(),
                        Money.vnd(item.unitPriceVnd()),
                        item.quantity()
                )
        );

        // 3. Place order — domain validates invariants, raises event
        order.place();

        // 4. Persist qua outbound port (interface)
        orderRepository.save(order);

        // 5. Publish domain events (collected từ aggregate)
        List<DomainEvent> events = order.pullDomainEvents();
        events.forEach(event -> {
            log.info("Publishing domain event: {}", event.getClass().getSimpleName());
            eventPublisher.publishEvent(event);   // Spring in-process events
        });

        log.info("Order placed successfully: {}", order.orderId());
        return order.orderId();
    }

    // ── Command ───────────────────────────────────────────────────────────
    public record PlaceOrderCommand(
            String customerId,
            List<OrderItemDto> items
    ) {
        public record OrderItemDto(
                String productId,
                String productName,
                long   unitPriceVnd,
                int    quantity
        ) {}
    }

    // ── Outbound Port (interface) ─────────────────────────────────────────
    // Application Layer chỉ biết interface này — KHÔNG biết JPA, SQL, MongoDB
    public interface OrderRepositoryPort {
        void save(OrderAggregate order);
        java.util.Optional<OrderAggregate> findById(OrderId orderId);
    }
}
