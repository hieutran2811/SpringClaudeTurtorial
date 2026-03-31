package com.example.springclaudeturtorial.phase10.ddd.domain.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * ============================================================
 * PHASE 10 — DDD: Value Object
 * ============================================================
 *
 * Value Object: đối tượng định danh bằng GIÁ TRỊ, không phải identity.
 *
 *   Money(100, "VND") == Money(100, "VND")  → true (by value)
 *   Entity order1 == Entity order2          → false (same data ≠ same entity)
 *
 * Đặc điểm bắt buộc của Value Object:
 *   1. IMMUTABLE — không thể thay đổi sau khi tạo
 *      → mọi "thay đổi" tạo ra object MỚI
 *   2. EQUALITY BY VALUE — equals() so sánh tất cả fields
 *   3. SELF-VALIDATING — validate trong constructor, không để state invalid
 *   4. NO IDENTITY — không có ID
 *
 * Dùng record thay class thông thường:
 *   → auto-generate: equals(), hashCode(), toString(), getters
 *   → immutable by default (fields final)
 *
 * Ví dụ Value Objects khác: Email, PhoneNumber, Address, Quantity, DateRange
 * ============================================================
 */
public record Money(BigDecimal amount, String currency) {

    // ── Self-validation trong compact constructor ─────────────────────────
    public Money {
        Objects.requireNonNull(amount, "Amount must not be null");
        Objects.requireNonNull(currency, "Currency must not be null");
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative: " + amount);
        }
        if (currency.isBlank()) {
            throw new IllegalArgumentException("Currency must not be blank");
        }
        // Normalize: 2 decimal places
        amount = amount.setScale(2, RoundingMode.HALF_UP);
    }

    // ── Factory methods ───────────────────────────────────────────────────
    public static Money of(long amount, String currency) {
        return new Money(BigDecimal.valueOf(amount), currency);
    }

    public static Money vnd(long amount) {
        return new Money(BigDecimal.valueOf(amount), "VND");
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    // ── Behavior — tạo object MỚI, không mutate ──────────────────────────
    public Money add(Money other) {
        assertSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        assertSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Cannot subtract: result would be negative (" + result + ")");
        }
        return new Money(result, this.currency);
    }

    public Money multiply(int factor) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(factor)), this.currency);
    }

    public boolean isGreaterThan(Money other) {
        assertSameCurrency(other);
        return this.amount.compareTo(other.amount) > 0;
    }

    private void assertSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public String toString() {
        return String.format("%,.0f %s", amount, currency);
    }
}
