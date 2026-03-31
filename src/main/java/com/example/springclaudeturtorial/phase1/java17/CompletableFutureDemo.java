package com.example.springclaudeturtorial.phase1.java17;

import java.util.List;
import java.util.concurrent.*;

/**
 * TOPIC: CompletableFuture — Async Non-blocking Programming
 *
 * Nền tảng để hiểu: Spring @Async, WebFlux Mono/Flux, reactive patterns.
 */
public class CompletableFutureDemo {

    // Giả lập các service gọi DB / HTTP có latency
    static String fetchUser(Long userId) {
        sleep(200);
        return "User[" + userId + ":Hieu]";
    }

    static List<String> fetchOrders(String userName) {
        sleep(300);
        return List.of("ORD-101", "ORD-102", "ORD-103");
    }

    static double fetchUserBalance(Long userId) {
        sleep(150);
        return 5_000_000.0;
    }

    static String fetchProductInfo(String orderId) {
        sleep(100);
        if (orderId.equals("ORD-999")) throw new RuntimeException("Order not found: " + orderId);
        return "Product for " + orderId;
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }


    // ── A. Cơ bản: supplyAsync, thenApply, thenAccept ────────────────────────
    static void basicDemo() throws Exception {
        System.out.println("\n[A] Basic CompletableFuture");
        long start = System.currentTimeMillis();

        // supplyAsync: chạy task trả về giá trị trên ForkJoinPool
        CompletableFuture<String> userFuture = CompletableFuture
            .supplyAsync(() -> fetchUser(1L));

        // thenApply: transform kết quả (như Stream.map) — KHÔNG block
        CompletableFuture<Integer> nameLengthFuture = userFuture
            .thenApply(user -> user.length());

        // thenAccept: consume kết quả, không trả về gì — side effect
        CompletableFuture<Void> printFuture = userFuture
            .thenAccept(user -> System.out.println("  Got user: " + user));

        // join(): block thread hiện tại cho đến khi xong (dùng khi cần kết quả)
        Integer length = nameLengthFuture.join();
        printFuture.join();
        System.out.println("  Name length: " + length);
        System.out.println("  Time: " + (System.currentTimeMillis() - start) + "ms");
    }


    // ── B. thenCompose — chain async tasks (sequential) ──────────────────────
    static void composeDemo() throws Exception {
        System.out.println("\n[B] thenCompose — Sequential Async Chain");
        long start = System.currentTimeMillis();

        // Fetch user THEN fetch orders của user đó (phải có user trước)
        CompletableFuture<List<String>> ordersFuture = CompletableFuture
            .supplyAsync(() -> fetchUser(1L))               // ~200ms
            .thenCompose(user ->                            // nhận user, trả CompletableFuture mới
                CompletableFuture.supplyAsync(() -> fetchOrders(user)) // ~300ms
            );

        List<String> orders = ordersFuture.join();
        System.out.println("  Orders: " + orders);
        System.out.println("  Time (sequential): " + (System.currentTimeMillis() - start) + "ms (~500ms)");

        // thenApply vs thenCompose:
        //   thenApply:   A → B          (transform, result là CompletableFuture<B>)
        //   thenCompose: A → Future<B>  (chain, result là CompletableFuture<B>, KHÔNG phải Future<Future<B>>)
    }


    // ── C. thenCombine — 2 task song song, gộp kết quả ───────────────────────
    static void combineDemo() throws Exception {
        System.out.println("\n[C] thenCombine — Parallel + Combine");
        long start = System.currentTimeMillis();

        // Chạy song song: fetch user (~200ms) VÀ fetch balance (~150ms)
        CompletableFuture<String> userFuture    = CompletableFuture.supplyAsync(() -> fetchUser(1L));
        CompletableFuture<Double> balanceFuture = CompletableFuture.supplyAsync(() -> fetchUserBalance(1L));

        // Khi CẢ 2 xong → gộp kết quả
        CompletableFuture<String> combined = userFuture.thenCombine(
            balanceFuture,
            (user, balance) -> user + " | Balance: " + String.format("%,.0f đ", balance)
        );

        System.out.println("  Result: " + combined.join());
        System.out.println("  Time (parallel): " + (System.currentTimeMillis() - start) + "ms (~200ms, không phải 350ms)");
    }


    // ── D. allOf — chờ tất cả tasks hoàn thành ───────────────────────────────
    static void allOfDemo() throws Exception {
        System.out.println("\n[D] allOf — Wait for All");
        long start = System.currentTimeMillis();

        List<String> orderIds = List.of("ORD-101", "ORD-102", "ORD-103");

        // Gọi fetchProductInfo cho tất cả orders song song
        List<CompletableFuture<String>> futures = orderIds.stream()
            .map(id -> CompletableFuture.supplyAsync(() -> fetchProductInfo(id)))
            .toList();

        // allOf: trả về Future<Void> — hoàn thành khi tất cả xong
        CompletableFuture<Void> allDone = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture[0])
        );

        // Sau khi allOf done, lấy kết quả từng future
        List<String> products = allDone.thenApply(v ->
            futures.stream().map(CompletableFuture::join).toList()
        ).join();

        System.out.println("  Products: " + products);
        System.out.println("  Time (all parallel ~100ms): " + (System.currentTimeMillis() - start) + "ms");
    }


    // ── E. exceptionally & handle — xử lý lỗi ────────────────────────────────
    static void errorHandlingDemo() throws Exception {
        System.out.println("\n[E] Error Handling");

        // exceptionally: fallback khi exception xảy ra
        String result1 = CompletableFuture
            .supplyAsync(() -> fetchProductInfo("ORD-999"))  // sẽ throw exception
            .exceptionally(ex -> {
                System.out.println("  exceptionally caught: " + ex.getMessage());
                return "DEFAULT_PRODUCT";                     // giá trị fallback
            })
            .join();
        System.out.println("  exceptionally result: " + result1);

        // handle: xử lý cả success VÀ failure (như try-catch-finally)
        String result2 = CompletableFuture
            .supplyAsync(() -> fetchProductInfo("ORD-999"))
            .handle((value, ex) -> {
                if (ex != null) {
                    System.out.println("  handle caught: " + ex.getMessage());
                    return "HANDLED_DEFAULT";
                }
                return value;
            })
            .join();
        System.out.println("  handle result: " + result2);

        // whenComplete: side effect sau khi xong (log, metrics) — KHÔNG thay đổi kết quả
        CompletableFuture
            .supplyAsync(() -> fetchUser(1L))
            .whenComplete((user, ex) -> {
                if (ex == null) System.out.println("  whenComplete: success — " + user);
                else            System.out.println("  whenComplete: failed — " + ex.getMessage());
            })
            .join();
    }


    // ── F. Custom Executor ────────────────────────────────────────────────────
    static void customExecutorDemo() throws Exception {
        System.out.println("\n[F] Custom Executor (production pattern)");

        // ForkJoinPool.commonPool() là default — dùng chung, có thể bị starved
        // Production: dùng dedicated ThreadPool cho IO-bound tasks
        ExecutorService ioExecutor = Executors.newFixedThreadPool(10);

        CompletableFuture<String> future = CompletableFuture
            .supplyAsync(() -> fetchUser(1L), ioExecutor)  // chạy trên ioExecutor
            .thenApplyAsync(user -> user.toUpperCase(), ioExecutor);

        System.out.println("  Result: " + future.join());
        ioExecutor.shutdown();

        // SA note: Spring @Async dùng TaskExecutor được config trong Spring context
        // WebFlux dùng Schedulers (bounded elastic cho IO)
    }


    public static void main(String[] args) throws Exception {
        System.out.println("═══════════════════════════════");
        System.out.println("  COMPLETABLE FUTURE DEMO");
        System.out.println("═══════════════════════════════");

        basicDemo();
        composeDemo();
        combineDemo();
        allOfDemo();
        errorHandlingDemo();
        customExecutorDemo();

        System.out.println("\n── Key Takeaways ──");
        System.out.println("  ✓ supplyAsync: chạy task async, trả về CompletableFuture<T>");
        System.out.println("  ✓ thenApply:   transform kết quả (sync)");
        System.out.println("  ✓ thenCompose: chain async task (tránh Future<Future<T>>)");
        System.out.println("  ✓ thenCombine: 2 task song song → gộp kết quả");
        System.out.println("  ✓ allOf:       chờ tất cả song song → giảm latency");
        System.out.println("  ✓ exceptionally/handle: luôn xử lý lỗi trong async code");
        System.out.println("  ✓ join() block thread — dùng ở edge (controller), không dùng giữa chain");
        System.out.println("  ✓ Spring @Async wraps method trong CompletableFuture tự động");
    }
}
