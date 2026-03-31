package com.example.springclaudeturtorial.phase5.reactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Chạy demo khi app khởi động (chỉ chạy ở profile local).
 * Tắt bằng cách comment @Component hoặc đổi profile.
 */
@Component
@Profile("local")
public class Phase5ReactiveRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(Phase5ReactiveRunner.class);

    // WebClientDemo inject riêng để demo thực tế với HTTP
    private final WebClientDemo webClientDemo;

    public Phase5ReactiveRunner(WebClientDemo webClientDemo) {
        this.webClientDemo = webClientDemo;
    }

    @Override
    public void run(String... args) {
        log.info("");
        log.info("╔══════════════════════════════════════════╗");
        log.info("║   PHASE 5 — Reactive Programming Demo   ║");
        log.info("╚══════════════════════════════════════════╝");

        // ── Reactor fundamentals ──────────────────────────────────────────
        MonoFluxDemo.monoBasics();
        MonoFluxDemo.fluxBasics();
        MonoFluxDemo.combining();
        MonoFluxDemo.errorHandling();
        MonoFluxDemo.schedulers();

        // ── WebClient — gọi HTTP thực ─────────────────────────────────────
        log.info("─── WebClient: Single request ───");
        webClientDemo.getPost(1)
                .doOnNext(p -> log.info("  Post title: {}", p.title()))
                .block();

        log.info("─── WebClient: Parallel requests ───");
        webClientDemo.getMultiplePostsInParallel()
                .doOnNext(posts -> posts.forEach(p ->
                        log.info("  Parallel result: [{}] {}", p.id(), p.title())))
                .block();

        log.info("─── WebClient: Stream of posts ───");
        webClientDemo.getPosts()
                .take(3)
                .doOnNext(p -> log.info("  Stream item: [{}] {}", p.id(), p.title()))
                .collectList()
                .block();

        log.info("");
        log.info("✓ Phase 5 Reactive Demo completed");
        log.info("");
    }
}
