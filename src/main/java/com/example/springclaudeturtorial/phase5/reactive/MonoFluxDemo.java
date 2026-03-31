package com.example.springclaudeturtorial.phase5.reactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.List;

/**
 * ============================================================
 * PHASE 5 — WebFlux Reactive: Mono & Flux
 * ============================================================
 *
 * Reactive Programming = lập trình bất đồng bộ theo mô hình Publisher/Subscriber.
 *
 * Hai kiểu Publisher trong Reactor:
 *
 *   Mono<T>  → 0 hoặc 1 phần tử   (như Optional nhưng async)
 *   Flux<T>  → 0 đến N phần tử     (như Stream nhưng async)
 *
 * QUAN TRỌNG: Nothing happens until you subscribe!
 *   Mono/Flux là "cold" publisher — chỉ chạy khi có subscriber.
 *   Khác với CompletableFuture (eager — chạy ngay khi tạo).
 *
 * ── MVC vs WebFlux ────────────────────────────────────────
 *   Spring MVC  : blocking  — 1 thread xử lý 1 request (thread-per-request)
 *   WebFlux     : non-blocking — ít thread xử lý nhiều request (event loop)
 *
 * Khi nào dùng WebFlux?
 *   ✓ Nhiều I/O-bound calls song song (gọi nhiều service cùng lúc)
 *   ✓ Streaming data (SSE, WebSocket)
 *   ✓ Cần throughput cao với ít tài nguyên
 *   ✗ CPU-bound work → dùng MVC với thread pool
 *   ✗ Team chưa quen reactive → MVC dễ debug hơn nhiều
 *
 * Lưu ý trong project này: spring-boot-starter-web + webflux cùng tồn tại
 * → Spring Boot ưu tiên WebMVC làm web layer.
 * WebFlux ở đây cung cấp Reactor (Mono/Flux) và WebClient.
 * ============================================================
 */
public class MonoFluxDemo {

    private static final Logger log = LoggerFactory.getLogger(MonoFluxDemo.class);

    // ══════════════════════════════════════════════════════════════════
    // PHẦN 1: Mono — 0 hoặc 1 phần tử
    // ══════════════════════════════════════════════════════════════════

    public static void monoBasics() {
        log.info("─── Mono Basics ───");

        // Tạo Mono
        Mono<String> hello = Mono.just("Hello Reactor");
        Mono<String> empty = Mono.empty();
        Mono<String> error = Mono.error(new RuntimeException("Oops"));

        // map: transform giá trị (sync)
        Mono<Integer> length = hello.map(String::length);

        // flatMap: transform → trả về Mono khác (async, dùng khi cần gọi I/O)
        Mono<String> upper = hello.flatMap(s ->
                Mono.just(s.toUpperCase())
        );

        // defaultIfEmpty: fallback khi Mono rỗng
        Mono<String> fallback = empty.defaultIfEmpty("Default Value");

        // onErrorReturn: fallback khi có lỗi
        Mono<String> safe = error.onErrorReturn("Recovered");

        // doOnNext: side-effect (không thay đổi giá trị, dùng để log/debug)
        Mono<String> withLog = hello
                .doOnNext(v  -> log.info("  Before map: {}", v))
                .map(String::toUpperCase)
                .doOnNext(v  -> log.info("  After map:  {}", v));

        // block(): subscribe và chờ kết quả — CHỈ dùng ở test hoặc main thread
        // KHÔNG bao giờ block() trong reactive pipeline hay web handler
        log.info("  length:   {}", length.block());
        log.info("  upper:    {}", upper.block());
        log.info("  fallback: {}", fallback.block());
        log.info("  safe:     {}", safe.block());
        withLog.block();
    }

    // ══════════════════════════════════════════════════════════════════
    // PHẦN 2: Flux — 0 đến N phần tử
    // ══════════════════════════════════════════════════════════════════

    public static void fluxBasics() {
        log.info("─── Flux Basics ───");

        // Tạo Flux
        Flux<Integer> numbers  = Flux.just(1, 2, 3, 4, 5);
        Flux<Integer> range    = Flux.range(1, 5);              // 1, 2, 3, 4, 5
        Flux<String>  fromList = Flux.fromIterable(
                List.of("Spring", "Boot", "Reactor"));

        // filter: giữ lại phần tử thoả điều kiện
        Flux<Integer> evens = numbers.filter(n -> n % 2 == 0);  // 2, 4

        // map: transform từng phần tử
        Flux<String> strings = range.map(n -> "item-" + n);     // item-1 ... item-5

        // flatMap: mỗi phần tử → Flux mới (có thể chạy song song)
        Flux<String> expanded = Flux.just("A", "B")
                .flatMap(letter -> Flux.just(letter + "1", letter + "2"));
        // kết quả: A1, A2, B1, B2 (thứ tự có thể khác vì song song)

        // concatMap: giống flatMap nhưng giữ thứ tự (sequential)
        Flux<String> ordered = Flux.just("A", "B")
                .concatMap(letter -> Flux.just(letter + "1", letter + "2"));
        // kết quả đảm bảo: A1, A2, B1, B2

        // reduce: gộp thành 1 giá trị (như Stream.reduce)
        Mono<Integer> sum = numbers.reduce(0, Integer::sum);    // Mono<15>

        // collectList: Flux → Mono<List>
        Mono<List<Integer>> list = evens.collectList();         // Mono<[2,4]>

        // take: lấy N phần tử đầu
        Flux<Integer> first3 = range.take(3);                   // 1, 2, 3

        // subscribe: đây là cách "bật" pipeline trong production code
        numbers
            .filter(n -> n % 2 == 0)
            .map(n -> n * 10)
            .subscribe(
                value -> log.info("  onNext:     {}", value),
                error -> log.error("  onError:    {}", error.getMessage()),
                ()    -> log.info("  onComplete: done")
            );

        log.info("  sum:      {}", sum.block());
        log.info("  list:     {}", list.block());
        log.info("  strings:  {}", strings.collectList().block());
        log.info("  expanded: {}", expanded.collectList().block());
    }

    // ══════════════════════════════════════════════════════════════════
    // PHẦN 3: Combining — zip, merge, concat
    // ══════════════════════════════════════════════════════════════════

    public static void combining() {
        log.info("─── Combining Streams ───");

        Flux<String> names  = Flux.just("Alice", "Bob", "Charlie");
        Flux<Integer> ages  = Flux.just(30, 25, 35);

        // zip: ghép từng cặp phần tử từ 2 Flux (dừng khi 1 trong 2 hết)
        Flux<String> zipped = Flux.zip(names, ages,
                (name, age) -> name + " is " + age);
        log.info("  zipped: {}", zipped.collectList().block());
        // [Alice is 30, Bob is 25, Charlie is 35]

        // merge: kết hợp 2 Flux, phần tử đến theo thứ tự thời gian (interleaved)
        Flux<String> merged = Flux.merge(
                Flux.just("A1", "A2"),
                Flux.just("B1", "B2")
        );
        log.info("  merged: {}", merged.collectList().block());

        // concat: nối 2 Flux tuần tự (xong cái trước rồi mới sang cái sau)
        Flux<String> concatenated = Flux.concat(
                Flux.just("A1", "A2"),
                Flux.just("B1", "B2")
        );
        log.info("  concat: {}", concatenated.collectList().block());
        // [A1, A2, B1, B2] — luôn theo thứ tự
    }

    // ══════════════════════════════════════════════════════════════════
    // PHẦN 4: Error Handling
    // ══════════════════════════════════════════════════════════════════

    public static void errorHandling() {
        log.info("─── Error Handling ───");

        Flux<Integer> risky = Flux.just(1, 2, 0, 4)
                .map(n -> {
                    if (n == 0) throw new ArithmeticException("Division by zero");
                    return 100 / n;
                });

        // onErrorReturn: trả về giá trị mặc định khi lỗi, stream dừng
        risky.onErrorReturn(-1)
             .subscribe(v -> log.info("  onErrorReturn: {}", v));
        // 100, 50, -1 (stream dừng ở đây)

        // onErrorResume: chuyển sang Flux dự phòng khi lỗi
        risky.onErrorResume(e -> {
                    log.warn("  Caught: {}", e.getMessage());
                    return Flux.just(0);
                })
             .subscribe(v -> log.info("  onErrorResume: {}", v));

        // retry: thử lại N lần trước khi propagate error
        Mono.error(new RuntimeException("temporary"))
            .retry(2)
            .onErrorReturn("gave up")
            .subscribe(v -> log.info("  retry result: {}", v));
    }

    // ══════════════════════════════════════════════════════════════════
    // PHẦN 5: Schedulers — chạy trên thread nào?
    // ══════════════════════════════════════════════════════════════════

    public static void schedulers() {
        log.info("─── Schedulers ───");

        // Schedulers.boundedElastic(): dành cho blocking I/O (DB, file, HTTP blocking)
        // Tạo thread pool co giãn, phù hợp wrap code blocking thành reactive
        Mono<String> fromBlocking = Mono.fromCallable(() -> {
                    // Giả lập blocking call (vd: JDBC query)
                    Thread.sleep(10);
                    return "result from blocking call";
                })
                .subscribeOn(Schedulers.boundedElastic());  // chạy trên pool riêng

        // publishOn: chuyển các operator SAU nó sang thread khác
        // subscribeOn: chỉ định thread cho toàn bộ source

        Flux.range(1, 3)
            .subscribeOn(Schedulers.parallel())         // source chạy trên parallel pool
            .map(n -> n * 2)
            .publishOn(Schedulers.boundedElastic())     // operator sau đây chạy trên elastic
            .map(n -> "[" + Thread.currentThread().getName() + "] " + n)
            .subscribe(v -> log.info("  scheduler: {}", v));

        // Chờ một chút để async hoàn thành
        try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        log.info("  blocking result: {}", fromBlocking.block());
    }
}
