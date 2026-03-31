package com.example.springclaudeturtorial.phase6.resilience;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ============================================================
 * PHASE 6 — Resilience4j: Circuit Breaker + Retry + Rate Limiter
 * ============================================================
 *
 * Resilience patterns giải quyết bài toán: "Chuyện gì xảy ra khi
 * service bên ngoài chậm hoặc down?"
 *
 * Các pattern và khi nào dùng:
 *
 * ┌─────────────────┬──────────────────────────────────────────────┐
 * │ Pattern         │ Dùng khi                                     │
 * ├─────────────────┼──────────────────────────────────────────────┤
 * │ Retry           │ Lỗi tạm thời (network blip, 503 momentary)  │
 * │ Circuit Breaker │ Service down kéo dài → fail fast, tiết kiệm │
 * │                 │ thread thay vì chờ timeout                   │
 * │ Rate Limiter    │ Giới hạn call-rate đến service/API bên ngoài │
 * │ Bulkhead        │ Cô lập resource, 1 service chậm không chặn  │
 * │                 │ toàn bộ thread pool                          │
 * │ Time Limiter    │ Đặt timeout cứng cho operation               │
 * └─────────────────┴──────────────────────────────────────────────┘
 *
 * Thứ tự AOP khi dùng nhiều annotation trên cùng 1 method:
 *   RateLimiter → CircuitBreaker → Retry → TimeLimiter → Bulkhead → method()
 * ============================================================
 */
@Service
public class ExternalApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);

    private final WebClient webClient;

    // Đếm số lần gọi thực để demo retry
    private final AtomicInteger callCount = new AtomicInteger(0);

    public ExternalApiService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://jsonplaceholder.typicode.com")
                .build();
    }

    // ── 1. Circuit Breaker ────────────────────────────────────────────────
    //
    // @CircuitBreaker(name = "externalApi", fallbackMethod = "fallbackPost")
    //
    // Circuit Breaker có 3 trạng thái:
    //
    //   CLOSED   → hoạt động bình thường, request đi qua
    //              (tên "closed" = mạch điện kín = thông)
    //
    //   OPEN     → mạch ngắt, reject request ngay (không gọi service)
    //              → trả về fallback thay vì để thread chờ timeout
    //              → sau wait-duration → chuyển sang HALF_OPEN
    //
    //   HALF_OPEN → cho N request thử vào
    //              → nếu đủ tỉ lệ thành công → CLOSED lại
    //              → nếu vẫn fail → OPEN trở lại
    //
    //   CLOSED ──[failure ≥ threshold]──► OPEN
    //     ▲                                 │
    //     │                          [wait-duration]
    //     │                                 │
    //     └──[success]──── HALF_OPEN ◄──────┘
    //
    @CircuitBreaker(name = "externalApi", fallbackMethod = "fallbackPost")
    public String getPostWithCircuitBreaker(int id) {
        log.info("  [CB] Calling external API for post {}", id);
        return webClient.get()
                .uri("/posts/{id}", id)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                .block();
    }

    // Fallback method — phải có CÙNG tham số + thêm Throwable ở cuối
    // Được gọi khi: circuit OPEN, hoặc method ném exception không được handle
    private String fallbackPost(int id, Throwable ex) {
        log.warn("  [CB] Fallback triggered for post {} — reason: {}", id, ex.getMessage());
        return """
                {"id":%d,"title":"[Fallback] Service unavailable","body":"Cached or default content"}
                """.formatted(id);
    }

    // ── 2. Retry ──────────────────────────────────────────────────────────
    //
    // @Retry tự động thử lại khi method ném exception.
    // Kết hợp với Circuit Breaker: Retry thử → CB đếm failures.
    //
    // Lưu ý: Retry chạy SAU Circuit Breaker trong AOP chain.
    // Nếu CB đang OPEN → Retry không có tác dụng (CB reject trước).
    @Retry(name = "externalApi", fallbackMethod = "fallbackRetry")
    public String callWithRetry() {
        int attempt = callCount.incrementAndGet();
        log.info("  [Retry] Attempt #{}", attempt);

        // Giả lập lỗi tạm thời: lần 1 và 2 fail, lần 3 thành công
        if (attempt % 3 != 0) {
            throw new java.io.UncheckedIOException(
                    new java.io.IOException("Simulated transient error on attempt " + attempt));
        }

        callCount.set(0);  // reset cho lần test tiếp
        return "Success on attempt " + attempt;
    }

    private String fallbackRetry(Throwable ex) {
        log.warn("  [Retry] All attempts failed: {}", ex.getMessage());
        callCount.set(0);
        return "Fallback after all retries exhausted";
    }

    // ── 3. Rate Limiter ───────────────────────────────────────────────────
    //
    // @RateLimiter giới hạn số lần gọi trong một khoảng thời gian.
    // Dùng để bảo vệ service bên ngoài khỏi bị spam.
    // Khi vượt limit → ném RequestNotPermitted exception → gọi fallback.
    @RateLimiter(name = "externalApi", fallbackMethod = "fallbackRateLimit")
    public String callWithRateLimit(int requestNumber) {
        log.info("  [RateLimit] Processing request #{}", requestNumber);
        return "Request #" + requestNumber + " processed";
    }

    private String fallbackRateLimit(int requestNumber, Throwable ex) {
        log.warn("  [RateLimit] Request #{} rejected — rate limit exceeded", requestNumber);
        return "Request #" + requestNumber + " REJECTED (rate limit)";
    }

    // ── 4. Kết hợp nhiều patterns ─────────────────────────────────────────
    //
    // Thứ tự thực thi: RateLimiter → CircuitBreaker → Retry → method
    @RateLimiter(name = "externalApi")
    @CircuitBreaker(name = "externalApi", fallbackMethod = "fallbackCombined")
    @Retry(name = "externalApi")
    public String callWithAllPatterns(int id) {
        log.info("  [Combined] Calling with all resilience patterns for id={}", id);
        return webClient.get()
                .uri("/posts/{id}", id)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(3))
                .block();
    }

    private String fallbackCombined(int id, Throwable ex) {
        log.warn("  [Combined] Fallback for id={}: {}", id, ex.getMessage());
        return "{\"id\":" + id + ",\"title\":\"Combined fallback\"}";
    }
}
