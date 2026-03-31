package com.example.springclaudeturtorial.phase10.ddd.domain.valueobject;

import java.util.UUID;

/**
 * OrderId: Value Object đặc biệt — typed ID.
 *
 * Tại sao không dùng plain Long/String?
 *
 *   void processOrder(Long orderId, Long customerId) {
 *     // compile thành công dù truyền nhầm thứ tự!
 *   }
 *
 *   void processOrder(OrderId orderId, CustomerId customerId) {
 *     // compile ERROR nếu truyền nhầm → type safety!
 *   }
 *
 * UUID cho distributed system: không cần DB để generate,
 * không conflict giữa các service.
 */
public record OrderId(String value) {

    public OrderId {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("OrderId must not be blank");
    }

    public static OrderId generate() {
        return new OrderId(UUID.randomUUID().toString());
    }

    public static OrderId of(String value) {
        return new OrderId(value);
    }

    @Override
    public String toString() { return value; }
}
