# Spring Ecosystem Learning Roadmap — Solution Architect Track

> **Mục tiêu:** Nắm vững hệ sinh thái Spring từ cơ bản đến chuyên sâu để thiết kế và kiến trúc hệ thống enterprise-grade.
>
> **Cập nhật lần cuối:** 2026-03-31
> **Người học:** hieutv

---

## Tổng quan tiến trình

| Giai đoạn | Tên | Trạng thái | Tiến độ |
|-----------|-----|-----------|---------|
| Phase 1 | Java & OOP Foundation | ✅ Hoàn thành | 100% |
| Phase 2 | Spring Core & IoC/DI | ✅ Hoàn thành | 100% |
| Phase 3 | Spring Boot Essentials | ✅ Hoàn thành | 100% |
| Phase 4 | Spring Data & Persistence | ✅ Hoàn thành | 100% |
| Phase 5 | Spring Web & REST APIs | ✅ Hoàn thành | 100% |
| Phase 6 | Resilience & Observability | ✅ Hoàn thành | 100% |
| Phase 7 | Testing chuyên sâu | ✅ Hoàn thành | 100% |
| Phase 8 | Kafka Messaging | ✅ Hoàn thành | 100% |
| Phase 9 | Spring Batch | ✅ Hoàn thành | 100% |
| Phase 10 | SA Design Patterns & Architecture | ✅ Hoàn thành | 100% |

**Trạng thái ký hiệu:** ⬜ Chưa bắt đầu | 🔄 Đang học | ✅ Hoàn thành | ⏸ Tạm dừng

---

## Phase 1 — Java & OOP Foundation ✅

### 1.1 Java Core
- [x] Java 17+ features: Records, Sealed classes, Pattern matching
- [x] Generics, Lambdas, Stream API
- [x] Optional, CompletableFuture, async patterns
- [x] Exception handling best practices

### 1.2 OOP & Design Principles
- [x] SOLID principles
- [x] Design Patterns: Singleton, Factory, Builder, Strategy, Decorator, Proxy
- [x] Dependency Inversion — nền tảng của Spring IoC

### 1.3 Build Tools
- [x] Gradle: build.gradle, dependencies, tasks

**Files:** `phase1/java17/`, `phase1/solid/`, `phase1/patterns/`, `phase1/Phase1Runner.java`

---

## Phase 2 — Spring Core & IoC/DI ✅

### 2.1 IoC Container
- [x] ApplicationContext vs BeanFactory
- [x] Bean lifecycle: instantiation, initialization, destruction
- [x] Scopes: singleton, prototype

### 2.2 Dependency Injection
- [x] Constructor injection vs Field injection vs Setter injection
- [x] `@Component`, `@Service`, `@Repository`, `@Controller`
- [x] `@Autowired`, `@Qualifier`, `@Primary`
- [x] `@Configuration` và `@Bean`

### 2.3 AOP (Aspect-Oriented Programming)
- [x] Khái niệm: Join point, Pointcut, Advice, Aspect
- [x] `@Aspect`, `@Before`, `@After`, `@Around`
- [x] Ứng dụng: Logging, Transaction, Security

### 2.4 Spring Expression Language (SpEL)
- [x] Cú pháp cơ bản, dùng trong `@Value`
- [x] Conditional beans: `@Conditional`, `@Profile`

**Files:** `phase2/container/`, `phase2/di/`, `phase2/config/`, `phase2/aop/`, `phase2/Phase2Runner.java`

---

## Phase 3 — Spring Boot Essentials ✅

### 3.1 Auto-configuration
- [x] `@SpringBootApplication` = `@Configuration` + `@EnableAutoConfiguration` + `@ComponentScan`
- [x] Spring Boot Starters
- [x] `AutoConfiguration.imports`
- [x] Viết custom Auto-configuration (`SmsAutoConfiguration`)

### 3.2 Configuration Management
- [x] `application.yml` với multi-profile (local, production)
- [x] `@ConfigurationProperties` — type-safe config binding (`AppProperties`)
- [x] Config hierarchy & externalized config

### 3.3 Spring Boot Actuator
- [x] Endpoints: `/health`, `/info`, `/metrics`, `/env`
- [x] Custom `HealthIndicator` (`PaymentGatewayHealthIndicator`)
- [x] Custom `InfoContributor` (`AppInfoContributor`)

### 3.4 Testing
- [x] `@SpringBootTest`, `@WebMvcTest`, `@DataJpaTest`
- [x] MockMvc, `@MockBean`

**Files:** `phase3/autoconfig/`, `phase3/properties/`, `phase3/actuator/`, `phase3/product/`

---

## Phase 4 — Spring Data & Persistence ✅

### 4.1 Spring Data JPA
- [x] Entity, Repository: `JpaRepository`
- [x] JPQL, `@Query`, Named queries
- [x] Derived query methods
- [x] Pagination & Sorting: `Pageable`
- [x] Relationships: `@OneToMany`, `@ManyToMany`, fetch strategies
- [x] N+1 fix: `@EntityGraph`
- [x] Projection: interface projection (`ItemSummary`)
- [x] Optimistic locking: `@Version`

### 4.2 Transaction Management
- [x] `@Transactional`: propagation, isolation levels
- [x] Optimistic vs Pessimistic locking

### 4.3 Spring Cache
- [x] `@Cacheable`, `@CacheEvict`, `@CachePut`
- [x] `CacheConfig` với ConcurrentHashMap (InMemory)

### 4.4 Database Migration
- [x] Flyway: V1–V4 migrations

**Files:** `phase4/jpa/`, `phase4/transaction/`, `phase4/cache/`, `db/migration/V1–V4`

---

## Phase 5 — Spring Web & REST APIs ✅

### 5.1 Global Exception Handling
- [x] `@RestControllerAdvice`, `@ExceptionHandler`
- [x] `ApiError` — chuẩn hóa error response body
- [x] `ResourceNotFoundException` (404), `BusinessException` (409)
- [x] `MethodArgumentNotValidException` handler → field-level errors

### 5.2 REST Design & Bean Validation
- [x] Custom constraint annotation (`@ValidCategory`)
- [x] Request DTO vs Entity
- [x] Validation Groups (`OnCreate`, `OnUpdate`)
- [x] HTTP conventions: POST→201+Location, PATCH, 204 No Content
- [x] Pagination với `Page<T>`

### 5.3 Spring Security + JWT
- [x] `SecurityFilterChain` (stateless)
- [x] `JwtUtil`: generate/validate (jjwt 0.12.6)
- [x] `JwtAuthenticationFilter` — `OncePerRequestFilter`
- [x] `BCryptPasswordEncoder`
- [x] `@PreAuthorize`, `@EnableMethodSecurity`
- [x] `/api/auth/register`, `/api/auth/login`

### 5.4 WebFlux Reactive
- [x] `Mono`/`Flux` operators: map, flatMap, zip, merge, concat
- [x] Error handling: `onErrorReturn`, `onErrorResume`, `retry`
- [x] Schedulers: `boundedElastic`, `parallel`
- [x] `WebClient`: single, parallel, fallback, timeout

**Files:** `phase5/web/exception/`, `phase5/web/validation/`, `phase5/web/dto/`, `phase5/security/`, `phase5/reactive/`

---

## Phase 6 — Resilience & Observability ✅

### 6.1 Resilience4j
- [x] `@CircuitBreaker` — 3 states: CLOSED/OPEN/HALF_OPEN
- [x] `@Retry` với exponential backoff
- [x] `@RateLimiter`
- [x] Fallback methods, kết hợp nhiều patterns
- [x] `CircuitBreakerRegistry` — xem state runtime
- [x] Programmatic API: `CircuitBreakerConfig`, `RetryConfig`

### 6.2 Distributed Tracing & Metrics
- [x] Micrometer Tracing + Brave/Zipkin
- [x] `@NewSpan`, `@SpanTag` — custom spans
- [x] `Observation` API — đo metrics + traces cùng lúc
- [x] MDC log correlation: traceId/spanId trong mọi log line
- [x] `Counter`, `Timer` với `MeterRegistry`
- [x] Prometheus metrics endpoint (`/actuator/prometheus`)

**Files:** `phase6/resilience/`, `phase6/tracing/`

---

## Phase 7 — Testing chuyên sâu ✅

### 7.1 Fix tests sau Phase 5 (breaking changes)
- [x] `ProductServiceTest` — `ResourceNotFoundException`, `BusinessException`
- [x] `ProductControllerTest` — `@WithMockUser`, `@MockBean` security beans
- [x] `ProductIntegrationTest` — `@WithMockUser`, 409 Conflict

### 7.2 @WebMvcTest với Security
- [x] `@WithMockUser` / `@WithMockUser(roles="ADMIN")`
- [x] anonymous → 401, USER delete → 403, ADMIN delete → 204
- [x] CSRF token cho POST/DELETE
- [x] Validation errors với field-level assertions

### 7.3 Testcontainers
- [x] `@Testcontainers`, `@Container` (PostgreSQL)
- [x] `@DynamicPropertySource` — inject datasource từ container
- [x] `@AutoConfigureTestDatabase(replace = NONE)`
- [x] `@EmbeddedKafka` cho Kafka test

**Files:** `phase7/ProductV2ControllerSecurityTest.java`, `phase7/ProductRepositoryContainerTest.java`

---

## Phase 8 — Kafka Messaging ✅

### 8.1 Kafka Fundamentals
- [x] Topic, Partition, Offset, Consumer Group
- [x] Key-based partition routing → ordering guarantee
- [x] At-least-once delivery với Manual ACK
- [x] Idempotency check (`eventId`)

### 8.2 Producer
- [x] `KafkaTemplate` async send + callback
- [x] Custom headers (`RecordHeader`)
- [x] Fan-out pattern (1 event → nhiều topic)

### 8.3 Consumer
- [x] `@KafkaListener` với `concurrency=3`
- [x] Manual `Acknowledgment`
- [x] `@RetryableTopic` với exponential backoff
- [x] Dead Letter Topic (DLT) consumer
- [x] Đọc Kafka headers trong consumer

### 8.4 Error Handling
- [x] `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`
- [x] `FixedBackOff` retry config

**Files:** `phase8/messaging/event/`, `phase8/messaging/config/`, `phase8/messaging/producer/`, `phase8/messaging/consumer/`, `phase8/KafkaMessagingIntegrationTest.java`

---

## Phase 9 — Spring Batch ✅

### 9.1 Batch Fundamentals
- [x] `Job`, `Step`, `JobLauncher`, `JobRepository`
- [x] `ItemReader`, `ItemProcessor`, `ItemWriter`
- [x] Chunk-oriented processing (chunk size = 10)
- [x] `JobParameters` — runtime config, unique instance per run
- [x] `JobInstance` vs `JobExecution`

### 9.2 Readers & Writers
- [x] `JpaPagingItemReader` — đọc từ DB theo page
- [x] `FlatFileItemWriter` — ghi CSV với header
- [x] `BeanWrapperFieldExtractor` + `DelimitedLineAggregator`

### 9.3 Processor & Error Handling
- [x] `ItemProcessor<I, O>` — transform + filter (return null = skip)
- [x] Skip policy: `faultTolerant().skip(...).skipLimit(5)`
- [x] Retry policy: `retry(...).retryLimit(3)`
- [x] `RepeatStatus.FINISHED` cho Tasklet step

### 9.4 Listeners & Monitoring
- [x] `JobExecutionListener` — beforeJob / afterJob
- [x] `StepExecution` metrics: readCount, writeCount, skipCount
- [x] Spring Batch metadata tables: `BATCH_JOB_EXECUTION`, `BATCH_STEP_EXECUTION`

**Files:** `phase9/batch/config/`, `phase9/batch/processor/`, `phase9/batch/listener/`, `phase9/batch/dto/`, `phase9/batch/BatchController.java`
**Test:** `phase9/ProductExportJobTest.java` — `@SpringBatchTest`, `JobLauncherTestUtils`, launchStep()

---

## Phase 10 — SA Design Patterns & Architecture ✅

### 10.1 DDD — Domain-Driven Design
- [x] Value Object: immutable, equality by value, self-validating (`Money` record)
- [x] Entity vs Value Object: Entity có identity, Value Object không
- [x] Typed ID (`OrderId`) — tránh primitive obsession
- [x] Aggregate Root: "cửa ngõ duy nhất" enforce invariants
- [x] Rich Domain Model vs Anemic Domain Model
- [x] Domain Events: aggregate ghi nhận, Application Layer publish
- [x] `pullDomainEvents()` pattern: aggregate không biết đến Spring/Kafka

### 10.2 Hexagonal Architecture (Ports & Adapters)
- [x] Dependency Rule: Infrastructure → Application → Domain
- [x] Inbound Port: Use Case interface (PlaceOrderUseCase)
- [x] Outbound Port: Repository interface (OrderRepositoryPort) — Domain không import JPA
- [x] Application Service orchestrates: create → addItem → place → save → publish
- [x] Domain Layer thuần Java → test nhanh, không cần Spring context
- [x] So sánh với Layered Architecture: Hexagonal dùng Dependency Inversion

### 10.3 CQRS — Command Query Responsibility Segregation
- [x] Command Side: Domain Model (rich, normalized, transaction)
- [x] Query Side: Read Model (denormalized, optimized, cacheable)
- [x] Level 2: 2 service riêng — `CommandService` + `QueryService`
- [x] Projection: `@EventListener` cập nhật Read Model khi có Domain Event
- [x] `OrderReadModel` record: flat, precomputed `totalFormatted`
- [x] Khi nào dùng: read/write có performance requirements khác nhau

### 10.4 Outbox Pattern — Reliable Messaging
- [x] Vấn đề Dual Write: DB write + Kafka send có thể mất đồng bộ
- [x] Giải pháp: ghi `OutboxEvent` vào DB trong cùng transaction với domain data
- [x] `@TransactionalEventListener(phase = BEFORE_COMMIT)` — chạy trước commit
- [x] `OutboxRelay`: `@Scheduled` polling, đọc unpublished → publish Kafka → mark published
- [x] At-least-once guarantee → consumer cần idempotency
- [x] Production-grade: Debezium CDC thay vì polling

### 10.5 Unit Testing Domain Layer
- [x] Domain Layer test thuần Java — không cần Spring context
- [x] Test Value Object invariants: equality, immutability, negative amount, currency mismatch
- [x] Test Aggregate: place() raises event, empty order throws, below minimum throws
- [x] Test domain events: pullDomainEvents() clears after first pull
- [x] Lợi ích Hexagonal: Domain test không cần DB, Kafka, Spring

**Files:**
- `phase10/ddd/domain/valueobject/Money.java` — immutable Value Object (record)
- `phase10/ddd/domain/valueobject/OrderId.java` — Typed ID
- `phase10/ddd/domain/event/DomainEvent.java` — base interface
- `phase10/ddd/domain/event/OrderPlacedEvent.java` — domain event record
- `phase10/ddd/domain/aggregate/OrderAggregate.java` — Rich Aggregate Root + inner OrderLine
- `phase10/hexagonal/PlaceOrderUseCase.java` — Application Service + outbound port
- `phase10/cqrs/OrderQueryService.java` — Read Side + @EventListener projection
- `phase10/outbox/OutboxPattern.java` — OutboxEvent entity, Writer, Relay
**Test:** `phase10/OrderAggregateTest.java` — 9 pure Java unit tests, 2 @Nested classes

---

## Tài nguyên học tập

### Sách
| Sách | Tác giả | Phase | Đọc xong |
|------|---------|-------|---------|
| Spring in Action (6th ed) | Craig Walls | 2–5 | ⬜ |
| Cloud Native Spring in Action | Thomas Vitale | 8–9 | ⬜ |
| Spring Security in Action | Laurentiu Spilca | 6 | ⬜ |
| Designing Data-Intensive Applications | Martin Kleppmann | 10 | ⬜ |
| Domain-Driven Design | Eric Evans | 10 | ⬜ |

### Official Docs
- Spring Framework: https://docs.spring.io/spring-framework/reference/
- Spring Boot: https://docs.spring.io/spring-boot/docs/current/reference/html/
- Spring Security: https://docs.spring.io/spring-security/reference/
- Spring Kafka: https://docs.spring.io/spring-kafka/reference/

---

## Nhật ký học tập

### 2026-03-31 — Bắt đầu
- Khởi tạo project: `SpringClaudeTurtorial`
- Hoàn thành Phase 1–4 (foundation)

### 2026-03-31 — Phase 5–8
- Phase 5: GlobalExceptionHandler, REST Design, Security+JWT, WebFlux
- Phase 6: Resilience4j (CB/Retry/RateLimit), Micrometer Tracing + Zipkin
- Phase 7: Testing với Security, Testcontainers PostgreSQL + EmbeddedKafka
- Phase 8: Kafka Producer/Consumer, DLT, @RetryableTopic, Fan-out pattern

---

## Checklist SA Competency

Tự đánh giá khi hoàn thành roadmap:

- [x] Có thể giải thích Spring IoC container hoạt động như thế nào không dùng Spring Boot
- [x] Có thể thiết kế và implement REST API production-ready với security, validation, caching
- [x] Có thể chọn đúng messaging solution (Kafka) và implement event-driven patterns
- [x] Có thể implement resilience patterns (Circuit Breaker, Retry, Rate Limiter)
- [x] Có thể setup observability: distributed tracing, metrics, log correlation
- [x] Có thể viết test đầy đủ: unit, @WebMvcTest+Security, Testcontainers, @SpringBatchTest
- [x] Có thể implement Batch job: chunk processing, skip/retry, CSV export
- [ ] Có thể thiết kế microservices architecture end-to-end
- [ ] Có thể viết ADR và justify technology choices
- [ ] Có thể migrate monolith sang microservices có chiến lược
- [ ] Hiểu tradeoffs CAP theorem trong distributed systems
