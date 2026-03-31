# Learning Progress Tracker

> File theo dõi tiến trình học tập hàng ngày / hàng tuần.
> Cập nhật file này sau mỗi buổi học.

---

## Sprint hiện tại

**Sprint:** #2 | **Từ:** 2026-03-31 | **Đến:** 2026-03-31 ✅ HOÀN THÀNH
**Phase đã học:** Phase 10 — SA Design Patterns & Architecture
**Mục tiêu sprint:**
- [x] Hoàn thành 10.1 DDD: Value Object, Aggregate Root, Domain Events, Rich Domain Model
- [x] Hoàn thành 10.2 Hexagonal Architecture (Ports & Adapters, Dependency Rule)
- [x] Hoàn thành 10.3 CQRS: Command/Query separation, Read Model, Projection
- [x] Hoàn thành 10.4 Outbox Pattern: Dual Write problem, @TransactionalEventListener, Relay

---

## Log học tập

### Template (copy để dùng)
```
#### YYYY-MM-DD
- **Thời gian học:** ___ phút
- **Chủ đề:** 
- **Đã học được:**
  - 
- **Chưa hiểu / cần tìm hiểu thêm:**
  - 
- **Code viết hôm nay:** (link file hoặc mô tả)
- **Ngày mai học tiếp:**
```

---

#### 2026-03-31 — Sprint #1 (Phase 1–8)

- **Chủ đề:** Phase 1 → Phase 8 (toàn bộ foundation + production patterns)
- **Đã học được:**
  - **Phase 1:** Java 17 Records/Sealed/Pattern, SOLID, Design Patterns, CompletableFuture
  - **Phase 2:** Bean Lifecycle, DI, AOP, @Conditional
  - **Phase 3:** Auto-configuration, @ConfigurationProperties, Actuator, MockMvc tests
  - **Phase 4:** JPA relationships, N+1 fix, @EntityGraph, Flyway migrations, @Cacheable
  - **Phase 5.1:** GlobalExceptionHandler, ApiError, ResourceNotFoundException, BusinessException
  - **Phase 5.2:** Custom @ValidCategory, Validation Groups (OnCreate/OnUpdate), PATCH, Location header
  - **Phase 5.3:** JWT (jjwt 0.12.6), SecurityFilterChain stateless, @PreAuthorize, BCrypt
  - **Phase 5.4:** Mono/Flux operators, WebClient parallel calls, Schedulers
  - **Phase 6.1:** CircuitBreaker (3 states), Retry + exponential backoff, RateLimiter, DLT
  - **Phase 6.2:** @NewSpan, Observation API, MDC traceId, Counter/Timer, Prometheus
  - **Phase 7:** @WithMockUser, @MockBean security beans, 401 vs 403, Testcontainers PostgreSQL
  - **Phase 8:** KafkaTemplate async, @KafkaListener concurrency, Manual ACK, @RetryableTopic, Fan-out
  - **Phase 9:** Job/Step/Chunk, JpaPagingItemReader, FlatFileItemWriter CSV, Skip/Retry policy, @SpringBatchTest
- **Code viết hôm nay:** Toàn bộ `src/` — Phase 1–9 hoàn chỉnh
- **Ngày mai học tiếp:** Phase 10 — SA Design Patterns & Architecture

---

#### 2026-03-31 — Sprint #2 (Phase 10)

- **Chủ đề:** Phase 10 — SA Design Patterns & Architecture
- **Đã học được:**
  - **10.1 DDD Value Object:** `Money` record — immutable, equality by value, self-validating, currency-safe arithmetic
  - **10.1 DDD Aggregate Root:** `OrderAggregate` — Rich Domain Model, invariants (MAX_LINES, MIN_AMOUNT), domain events collection
  - **10.1 Domain Events:** `OrderPlacedEvent` — aggregate không publish trực tiếp, Application Layer publish sau khi save
  - **10.2 Hexagonal Architecture:** Domain thuần Java (no Spring/JPA), `OrderRepositoryPort` interface, Dependency Inversion
  - **10.2 Application Service:** `PlaceOrderUseCase.execute()` — orchestrate domain methods, persist, publish events
  - **10.3 CQRS:** `OrderQueryService` (read side) + `@EventListener` projection cập nhật Read Model từ Domain Event
  - **10.4 Outbox Pattern:** `@TransactionalEventListener(BEFORE_COMMIT)` ghi outbox cùng transaction, `@Scheduled` relay publish Kafka
  - **10.5 Testing:** 9 pure Java unit tests — domain test không cần Spring context (lợi ích của Hexagonal)
- **Code viết hôm nay:** `phase10/` — 8 main files + 1 test file
- **Ngày mai học tiếp:** Roadmap hoàn thành — Phase 1–10 ✅

---

## Thống kê tổng hợp

| Tuần | Phase hoàn thành | Ghi chú |
|------|-----------------|---------|
| 2026-W14 | Phase 1–10 ✅ | Sprint #1+2 — toàn bộ roadmap hoàn thành! |

---

## Các vấn đề đang mắc kẹt

| Ngày | Vấn đề | Trạng thái | Giải pháp |
|------|--------|-----------|---------|
| | | ⬜ Chưa có vấn đề nào | |

---

## Code snippets đáng nhớ

### JWT Authentication Filter pattern
```java
// OncePerRequestFilter — chạy đúng 1 lần/request
@Override
protected void doFilterInternal(HttpServletRequest req, ...) {
    String token = header.substring(7); // bỏ "Bearer "
    if (jwtUtil.isValid(token)) {
        var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    chain.doFilter(req, res);
}
```

### Circuit Breaker state machine
```
CLOSED ──[failure ≥ threshold]──► OPEN
  ▲                                  │
  │                          [wait-duration]
  └──[success]──── HALF_OPEN ◄───────┘
```

### @WebMvcTest với Security — 3 thứ cần nhớ
```java
@WebMvcTest(MyController.class)
@WithMockUser                           // ① mock user cho tất cả test
class Test {
    @MockBean JwtAuthenticationFilter f; // ② mock security beans
    @MockBean UserDetailsServiceImpl  u; //    để context khởi tạo được
    @MockBean JwtUtil jwtUtil;

    // ③ Override per-test: @WithMockUser(roles="ADMIN")
}
```

### Kafka consumer với Manual ACK + Idempotency
```java
@KafkaListener(topics = "orders", concurrency = "3")
public void handle(ConsumerRecord<String, OrderEvent> record, Acknowledgment ack) {
    if (processedEvents.putIfAbsent(event.eventId(), true) != null) {
        ack.acknowledge(); return; // duplicate → skip
    }
    // process...
    ack.acknowledge(); // commit offset sau khi xử lý xong
}
```

### Testcontainers với @DynamicPropertySource
```java
@Container
static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

@DynamicPropertySource
static void config(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
}
```

---

## Quyết định kiến trúc (ADR mini)

| # | Quyết định | Lý do chọn | Tradeoffs |
|---|-----------|-----------|---------|
| 1 | Stateless JWT (không Session) | REST API scalability, microservices-ready | Token không thể revoke ngay, cần blacklist nếu cần |
| 2 | Manual ACK cho Kafka consumer | At-least-once delivery đảm bảo hơn | Phải implement idempotency để tránh duplicate |
| 3 | Testcontainers PostgreSQL thay H2 | Test gần production nhất, tránh dialect mismatch | Cần Docker, test chậm hơn H2 |
| 4 | Separate DTO (ProductRequest) từ Entity | Kiểm soát API contract, validation độc lập | Thêm code mapping boilerplate |
| 5 | GlobalExceptionHandler thay inline @ExceptionHandler | DRY, format lỗi nhất quán toàn app | Cần `@RestControllerAdvice` context |
| 6 | Rich Domain Model (Aggregate) thay Anemic | Business rules nằm trong domain → encapsulated, testable | Cần thiết kế cẩn thận, học curve DDD |
| 7 | Outbox Pattern thay Dual Write | At-least-once guarantee, không mất event khi Kafka down | Cần thêm bảng outbox_events, polling overhead |
| 8 | Hexagonal Architecture (Ports & Adapters) | Domain thuần Java → test nhanh, dễ swap DB/Kafka | More boilerplate, interfaces cho mọi port |
