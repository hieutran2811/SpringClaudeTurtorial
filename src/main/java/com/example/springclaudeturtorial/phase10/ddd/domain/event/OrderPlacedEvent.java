package com.example.springclaudeturtorial.phase10.ddd.domain.event;

import com.example.springclaudeturtorial.phase10.ddd.domain.valueobject.Money;
import com.example.springclaudeturtorial.phase10.ddd.domain.valueobject.OrderId;

import java.time.Instant;
import java.util.UUID;

public record OrderPlacedEvent(
        String  eventId,
        Instant occurredAt,
        OrderId orderId,
        String  customerId,
        Money   totalAmount,
        int     itemCount
) implements DomainEvent {

    public static OrderPlacedEvent from(OrderId orderId, String customerId,
                                        Money total, int itemCount) {
        return new OrderPlacedEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                orderId, customerId, total, itemCount
        );
    }
}
