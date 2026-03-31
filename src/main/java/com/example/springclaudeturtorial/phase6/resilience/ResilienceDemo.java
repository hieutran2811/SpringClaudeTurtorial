package com.example.springclaudeturtorial.phase6.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * ============================================================
 * PHASE 6 — Resilience4j: Programmatic API (không cần Spring)
 * ============================================================
 *
 * Phần này dùng Resilience4j API trực tiếp (không qua annotation)
 * để hiểu rõ cơ chế bên trong.
 *
 * Annotation như @CircuitBreaker là shortcut — bên dưới Spring AOP
 * tự gọi những đoạn code tương tự phần này.
 * ============================================================
 */
public class ResilienceDemo {

    private static final Logger log = LoggerFactory.getLogger(ResilienceDemo.class);

    // ══════════════════════════════════════════════════════════════════
    // Circuit Breaker — Programmatic
    // ══════════════════════════════════════════════════════════════════
    public static void circuitBreakerProgrammatic() {
        log.info("─── Circuit Breaker (Programmatic) ───");

        // Tạo config
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(5)             // xem 5 call gần nhất
                .failureRateThreshold(60)          // ≥60% fail → OPEN
                .waitDurationInOpenState(Duration.ofSeconds(2))
                .build();

        CircuitBreaker cb = CircuitBreakerRegistry
                .ofDefaults()
                .circuitBreaker("demo", config);

        // Lắng nghe thay đổi trạng thái
        cb.getEventPublisher()
          .onStateTransition(e ->
              log.info("  [CB Event] State transition: {} → {}",
                       e.getStateTransition().getFromState(),
                       e.getStateTransition().getToState()));

        AtomicInteger attempt = new AtomicInteger(0);

        // Wrap function với circuit breaker
        Supplier<String> protectedCall = CircuitBreaker.decorateSupplier(cb, () -> {
            int n = attempt.incrementAndGet();
            if (n <= 4) throw new RuntimeException("Fail #" + n);
            return "Success on call #" + n;
        });

        // Gọi 7 lần: 4 fail (CB mở), rồi thêm vài call khi CB đang OPEN
        for (int i = 1; i <= 7; i++) {
            try {
                String result = protectedCall.get();
                log.info("  Call {}: SUCCESS — {}", i, result);
            } catch (Exception e) {
                log.warn("  Call {}: FAILED  — {} | CB state: {}", i, e.getMessage(), cb.getState());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════
    // Retry — Programmatic với exponential backoff
    // ══════════════════════════════════════════════════════════════════
    public static void retryWithBackoff() {
        log.info("─── Retry with Exponential Backoff ───");

        RetryConfig config = RetryConfig.custom()
                .maxAttempts(4)
                // Exponential backoff: 100ms → 200ms → 400ms
                .intervalFunction(io.github.resilience4j.core.IntervalFunction
                        .ofExponentialBackoff(100, 2.0))
                .retryExceptions(RuntimeException.class)
                .build();

        Retry retry = Retry.of("demo-retry", config);

        // Lắng nghe sự kiện retry
        retry.getEventPublisher()
             .onRetry(e -> log.info("  [Retry Event] Attempt #{} after {}ms",
                     e.getNumberOfRetryAttempts(),
                     e.getWaitInterval().toMillis()));

        AtomicInteger counter = new AtomicInteger(0);

        Supplier<String> protectedCall = Retry.decorateSupplier(retry, () -> {
            int n = counter.incrementAndGet();
            log.info("  Executing attempt #{}", n);
            if (n < 3) throw new RuntimeException("Transient error #" + n);
            return "Succeeded on attempt #" + n;
        });

        try {
            String result = protectedCall.get();
            log.info("  Final result: {}", result);
        } catch (Exception e) {
            log.error("  All retries failed: {}", e.getMessage());
        }
    }
}
