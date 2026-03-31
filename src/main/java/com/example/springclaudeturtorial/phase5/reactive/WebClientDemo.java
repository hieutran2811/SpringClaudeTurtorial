package com.example.springclaudeturtorial.phase5.reactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * ============================================================
 * PHASE 5 — WebClient: Reactive HTTP Client
 * ============================================================
 *
 * WebClient = reactive thay thế cho RestTemplate (deprecated trong Spring 5+).
 *
 * So sánh:
 *   RestTemplate  : blocking — thread chờ response
 *   WebClient     : non-blocking — thread không chờ, xử lý khi có response
 *
 * WebClient phù hợp khi cần gọi nhiều API song song (parallelism),
 * thay vì gọi tuần tự từng cái.
 *
 * Ví dụ thực tế:
 *   Product page cần: product info + reviews + recommendations + inventory
 *   → Gọi 4 service song song bằng Flux.zip() → tổng thời gian = max(4 calls)
 *   → Thay vì gọi tuần tự → tổng thời gian = sum(4 calls)
 * ============================================================
 */
@Component
public class WebClientDemo {

    private static final Logger log = LoggerFactory.getLogger(WebClientDemo.class);

    // WebClient.Builder được Spring auto-configure khi có webflux on classpath
    private final WebClient webClient;

    public WebClientDemo(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://jsonplaceholder.typicode.com")  // free mock API
                .build();
    }

    // ── Lấy 1 post theo ID ────────────────────────────────────────────────
    public Mono<Post> getPost(int id) {
        return webClient.get()
                .uri("/posts/{id}", id)
                .retrieve()
                // onStatus: xử lý HTTP error codes
                .onStatus(
                    status -> status.is4xxClientError(),
                    response -> Mono.error(new RuntimeException("Post not found: " + id))
                )
                .bodyToMono(Post.class)
                .timeout(Duration.ofSeconds(5))         // timeout 5s
                .doOnNext(p -> log.info("  Fetched post: [{}] {}", p.id(), p.title()))
                .onErrorReturn(new Post(id, 0, "Error fetching post", ""));
    }

    // ── Lấy danh sách posts ───────────────────────────────────────────────
    public Flux<Post> getPosts() {
        return webClient.get()
                .uri("/posts?_limit=5")
                .retrieve()
                .bodyToFlux(Post.class)
                .timeout(Duration.ofSeconds(5));
    }

    // ── Gọi song song nhiều requests — đây là điểm mạnh của reactive ──────
    //
    // Mono.zip(): gọi post 1, 2, 3 CÙNG LÚC (không đợi nhau)
    // Tổng thời gian ≈ max(t1, t2, t3) thay vì t1 + t2 + t3
    public Mono<List<Post>> getMultiplePostsInParallel() {
        log.info("─── Fetching 3 posts in parallel ───");

        Mono<Post> post1 = getPost(1);
        Mono<Post> post2 = getPost(2);
        Mono<Post> post3 = getPost(3);

        return Mono.zip(post1, post2, post3,
                (p1, p2, p3) -> List.of(p1, p2, p3));
    }

    // ── Xử lý lỗi nâng cao ────────────────────────────────────────────────
    public Mono<Post> getPostWithFallback(int id) {
        return getPost(id)
                .onErrorResume(e -> {
                    log.warn("  Failed to get post {}: {} — using fallback", id, e.getMessage());
                    return Mono.just(new Post(id, 0, "Fallback Post", "Content unavailable"));
                });
    }

    // Record cho JSON deserialization
    public record Post(int id, int userId, String title, String body) {}
}
