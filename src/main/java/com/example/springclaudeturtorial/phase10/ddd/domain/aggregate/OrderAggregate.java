package com.example.springclaudeturtorial.phase10.ddd.domain.aggregate;

import com.example.springclaudeturtorial.phase10.ddd.domain.event.DomainEvent;
import com.example.springclaudeturtorial.phase10.ddd.domain.event.OrderPlacedEvent;
import com.example.springclaudeturtorial.phase10.ddd.domain.valueobject.Money;
import com.example.springclaudeturtorial.phase10.ddd.domain.valueobject.OrderId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * ============================================================
 * PHASE 10 — DDD: Aggregate Root
 * ============================================================
 *
 * Aggregate = cluster of domain objects (Entities + Value Objects)
 *             được xử lý như một đơn vị nhất quán.
 *
 * Aggregate Root = "cửa ngõ duy nhất" vào aggregate.
 *   - Mọi thay đổi trong aggregate PHẢI đi qua root
 *   - Đảm bảo invariants (business rules) luôn được enforce
 *   - Tham chiếu từ ngoài chỉ được đến root, không đến inner entities
 *
 * Ví dụ:
 *   OrderAggregate (root)
 *     ├── OrderLine (entity trong aggregate) — truy cập qua Order
 *     └── Address  (value object)
 *
 *   Không cho phép: orderLineRepo.save(line) trực tiếp
 *   Chỉ cho phép:   orderRepo.save(order) → cascade
 *
 * ── Rich Domain Model vs Anemic Domain Model ─────────────
 *
 *   Anemic (anti-pattern):
 *     Order chỉ có getters/setters
 *     OrderService chứa toàn bộ business logic
 *     → logic rải rác, khó maintain, khó test
 *
 *   Rich (DDD):
 *     Order chứa behavior: place(), addItem(), cancel()
 *     Business rules nằm ngay trong domain class
 *     → self-documenting, encapsulated, testable
 *
 * ── Domain Events in Aggregate ───────────────────────────
 *   Aggregate tự ghi nhận events xảy ra
 *   Application Layer publish events sau khi save
 *   → Domain không biết đến infrastructure (Kafka, Spring)
 * ============================================================
 */
public class OrderAggregate {

    // ── Identity ──────────────────────────────────────────────────────────
    private final OrderId orderId;

    // ── State ─────────────────────────────────────────────────────────────
    private final String       customerId;
    private final List<OrderLine> orderLines;
    private OrderStatus        status;

    // ── Domain Events (collected, published by Application Layer) ─────────
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    // ── Invariants (business rules enforced by aggregate) ─────────────────
    private static final int    MAX_LINES    = 20;
    private static final Money  MIN_AMOUNT   = Money.vnd(10_000);

    private OrderAggregate(OrderId orderId, String customerId) {
        this.orderId    = orderId;
        this.customerId = customerId;
        this.orderLines = new ArrayList<>();
        this.status     = OrderStatus.DRAFT;
    }

    // ── Factory method (thay vì constructor public) ───────────────────────
    public static OrderAggregate create(String customerId) {
        if (customerId == null || customerId.isBlank())
            throw new IllegalArgumentException("CustomerId must not be blank");
        return new OrderAggregate(OrderId.generate(), customerId);
    }

    // ── Business Methods (Rich Domain Model) ─────────────────────────────

    /**
     * Thêm sản phẩm vào đơn hàng.
     * Enforce invariant: tối đa MAX_LINES dòng.
     */
    public void addItem(String productId, String productName, Money unitPrice, int quantity) {
        ensureStatus(OrderStatus.DRAFT);
        if (orderLines.size() >= MAX_LINES)
            throw new IllegalStateException("Order cannot have more than " + MAX_LINES + " lines");
        if (quantity <= 0)
            throw new IllegalArgumentException("Quantity must be positive");

        // Nếu đã có sản phẩm này → tăng quantity
        orderLines.stream()
                .filter(line -> line.productId().equals(productId))
                .findFirst()
                .ifPresentOrElse(
                        line -> line.increaseQuantity(quantity),
                        () -> orderLines.add(new OrderLine(productId, productName, unitPrice, quantity))
                );
    }

    /**
     * Đặt hàng — enforce business rule về minimum amount.
     * Publish domain event khi thành công.
     */
    public void place() {
        ensureStatus(OrderStatus.DRAFT);
        if (orderLines.isEmpty())
            throw new IllegalStateException("Cannot place empty order");

        Money total = calculateTotal();
        if (total.amount().compareTo(MIN_AMOUNT.amount()) < 0)
            throw new IllegalStateException(
                    "Order total " + total + " is below minimum " + MIN_AMOUNT);

        this.status = OrderStatus.PLACED;

        // Ghi nhận Domain Event — KHÔNG publish trực tiếp
        // Application Layer sẽ publish sau khi save
        domainEvents.add(OrderPlacedEvent.from(
                orderId, customerId, total, orderLines.size()));
    }

    /**
     * Hủy đơn — chỉ được hủy khi chưa được xác nhận.
     */
    public void cancel(String reason) {
        if (status == OrderStatus.CONFIRMED || status == OrderStatus.SHIPPED)
            throw new IllegalStateException(
                    "Cannot cancel order in status: " + status);
        this.status = OrderStatus.CANCELLED;
    }

    // ── Read-only access ──────────────────────────────────────────────────
    public Money calculateTotal() {
        return orderLines.stream()
                .map(OrderLine::lineTotal)
                .reduce(Money.vnd(0), Money::add);
    }

    public OrderId   orderId()       { return orderId; }
    public String    customerId()    { return customerId; }
    public OrderStatus status()      { return status; }
    public int       itemCount()     { return orderLines.size(); }

    // Trả về unmodifiable list
    public List<OrderLine> orderLines() {
        return Collections.unmodifiableList(orderLines);
    }

    // Application Layer gọi sau khi save để publish events
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }

    private void ensureStatus(OrderStatus expected) {
        if (this.status != expected)
            throw new IllegalStateException(
                    "Operation not allowed in status: " + this.status);
    }

    // ── Inner Entity: OrderLine ───────────────────────────────────────────
    // Entity nằm trong aggregate — không có repository riêng
    public static class OrderLine {
        private final String productId;
        private final String productName;
        private final Money  unitPrice;
        private int          quantity;

        public OrderLine(String productId, String productName, Money unitPrice, int quantity) {
            this.productId   = productId;
            this.productName = productName;
            this.unitPrice   = unitPrice;
            this.quantity    = quantity;
        }

        void increaseQuantity(int delta) { this.quantity += delta; }

        public Money    lineTotal()    { return unitPrice.multiply(quantity); }
        public String   productId()    { return productId; }
        public String   productName()  { return productName; }
        public Money    unitPrice()    { return unitPrice; }
        public int      quantity()     { return quantity; }
    }

    public enum OrderStatus { DRAFT, PLACED, CONFIRMED, SHIPPED, CANCELLED }
}
