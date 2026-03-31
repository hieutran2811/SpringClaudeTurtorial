package com.example.springclaudeturtorial.phase6.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.IntStream;

/**
 * ============================================================
 * PHASE 6 — Resilience Controller
 * ============================================================
 *
 * Endpoints để test và quan sát từng resilience pattern.
 *
 * Dùng kèm với Actuator để xem trạng thái circuit breaker:
 *   GET /actuator/circuitbreakers
 *   GET /actuator/health
 * ============================================================
 */
@RestController
@RequestMapping("/api/v1/resilience")
public class ResilienceController {

    private final ExternalApiService    externalApiService;
    private final CircuitBreakerRegistry cbRegistry;

    public ResilienceController(ExternalApiService externalApiService,
                                CircuitBreakerRegistry cbRegistry) {
        this.externalApiService = externalApiService;
        this.cbRegistry         = cbRegistry;
    }

    // ── 1. Test Circuit Breaker ───────────────────────────────────────────
    // GET /api/v1/resilience/circuit-breaker/{id}
    @GetMapping("/circuit-breaker/{id}")
    public ResponseEntity<String> testCircuitBreaker(@PathVariable int id) {
        String result = externalApiService.getPostWithCircuitBreaker(id);
        return ResponseEntity.ok(result);
    }

    // ── 2. Test Retry ─────────────────────────────────────────────────────
    // GET /api/v1/resilience/retry
    @GetMapping("/retry")
    public ResponseEntity<String> testRetry() {
        String result = externalApiService.callWithRetry();
        return ResponseEntity.ok(result);
    }

    // ── 3. Test Rate Limiter — gửi nhiều request liên tiếp ────────────────
    // GET /api/v1/resilience/rate-limit?count=8
    // Gửi 8 request với limit 5/giây → 3 request cuối bị reject
    @GetMapping("/rate-limit")
    public ResponseEntity<Map<String, Object>> testRateLimit(
            @RequestParam(defaultValue = "8") int count) {

        var results = IntStream.rangeClosed(1, count)
                .mapToObj(i -> externalApiService.callWithRateLimit(i))
                .toList();

        return ResponseEntity.ok(Map.of(
                "requestCount", count,
                "results", results
        ));
    }

    // ── 4. Xem trạng thái Circuit Breaker trực tiếp ───────────────────────
    // GET /api/v1/resilience/status
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCircuitBreakerStatus() {
        CircuitBreaker cb = cbRegistry.circuitBreaker("externalApi");

        var metrics = cb.getMetrics();
        return ResponseEntity.ok(Map.of(
                "state",              cb.getState().name(),
                "failureRate",        metrics.getFailureRate() + "%",
                "callsSucceeded",     metrics.getNumberOfSuccessfulCalls(),
                "callsFailed",        metrics.getNumberOfFailedCalls(),
                "callsNotPermitted",  metrics.getNumberOfNotPermittedCalls()
        ));
    }
}
