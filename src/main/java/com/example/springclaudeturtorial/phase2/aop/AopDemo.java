package com.example.springclaudeturtorial.phase2.aop;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;
import java.util.HashMap;
import java.util.Map;

/**
 * TOPIC: AOP — Aspect-Oriented Programming
 *
 * Giải quyết cross-cutting concerns: logging, timing, caching, security
 * mà không làm bẩn business logic.
 */
public class AopDemo {

    // ════════════════════════════════════════════════════════════════════════
    // Custom Annotation — dùng làm Pointcut target
    // ════════════════════════════════════════════════════════════════════════
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Timed {}   // đánh dấu method cần đo thời gian

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Cached {
        int ttlSeconds() default 60;
    }


    // ════════════════════════════════════════════════════════════════════════
    // Business Services — chỉ chứa business logic, KHÔNG có logging/timing
    // ════════════════════════════════════════════════════════════════════════
    @Service
    static class ProductService {

        @Timed
        public String getProduct(Long id) {
            sleep(50); // giả lập DB query
            if (id <= 0) throw new IllegalArgumentException("Invalid product id: " + id);
            return "Product-" + id;
        }

        @Timed
        @Cached(ttlSeconds = 120)
        public String getProductWithCache(Long id) {
            sleep(100); // giả lập slow DB query
            return "CachedProduct-" + id;
        }

        public void updateProduct(Long id, String name) {
            sleep(30);
            System.out.println("    [ProductService] Updated product " + id + " → " + name);
        }

        private void internalHelper() {
            // private method — AOP KHÔNG can thiệp được
            System.out.println("    [ProductService] internalHelper()");
        }
    }

    @Service
    static class OrderService {

        @Timed
        public String placeOrder(Long userId, Long productId, int qty) {
            sleep(80);
            return "Order{user=" + userId + ", product=" + productId + ", qty=" + qty + "}";
        }

        // Self-invocation demo — gọi method trong cùng class
        public void processWithSelfInvocation() {
            System.out.println("    [OrderService] processWithSelfInvocation()");
            this.placeOrder(1L, 1L, 1); // @Timed KHÔNG được apply — self-invocation!
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // ASPECT 1: Logging — log tất cả method trong service layer
    // ════════════════════════════════════════════════════════════════════════
    @Aspect
    @Component
    static class LoggingAspect {

        // Pointcut: tất cả method trong package này
        @Pointcut("execution(* com.example.springclaudeturtorial.phase2.aop.AopDemo.*Service.*(..))")
        public void serviceLayer() {}

        // Pointcut: method có annotation @Timed
        @Pointcut("@annotation(com.example.springclaudeturtorial.phase2.aop.AopDemo.Timed)")
        public void timedMethods() {}

        @Before("serviceLayer()")
        public void logBefore(JoinPoint jp) {
            String method = jp.getSignature().getName();
            Object[] args = jp.getArgs();
            System.out.println("  [LOG→] " + method + "(" + formatArgs(args) + ")");
        }

        @AfterReturning(pointcut = "serviceLayer()", returning = "result")
        public void logAfterSuccess(JoinPoint jp, Object result) {
            System.out.println("  [LOG←] " + jp.getSignature().getName()
                + " returned: " + result);
        }

        @AfterThrowing(pointcut = "serviceLayer()", throwing = "ex")
        public void logAfterError(JoinPoint jp, Exception ex) {
            System.out.println("  [LOG✗] " + jp.getSignature().getName()
                + " threw: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }

        private String formatArgs(Object[] args) {
            if (args == null || args.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append(args[i]);
            }
            return sb.toString();
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // ASPECT 2: Performance Timer — đo thời gian method có @Timed
    // ════════════════════════════════════════════════════════════════════════
    @Aspect
    @Component
    static class PerformanceAspect {

        @Around("@annotation(com.example.springclaudeturtorial.phase2.aop.AopDemo.Timed)")
        public Object measureTime(ProceedingJoinPoint pjp) throws Throwable {
            long start = System.currentTimeMillis();
            String method = pjp.getSignature().toShortString();
            try {
                Object result = pjp.proceed(); // gọi method thật
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("  [PERF] " + method + " took " + elapsed + "ms");
                return result;
            } catch (Throwable ex) {
                long elapsed = System.currentTimeMillis() - start;
                System.out.println("  [PERF] " + method + " FAILED after " + elapsed + "ms");
                throw ex;
            }
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // ASPECT 3: Caching — cache kết quả method có @Cached
    // ════════════════════════════════════════════════════════════════════════
    @Aspect
    @Component
    static class CachingAspect {
        private final Map<String, Object> cache = new HashMap<>();

        @Around("@annotation(cached)")
        public Object cacheResult(ProceedingJoinPoint pjp, Cached cached) throws Throwable {
            // Tạo cache key từ method name + arguments
            String key = pjp.getSignature().getName() + ":" + java.util.Arrays.toString(pjp.getArgs());

            if (cache.containsKey(key)) {
                System.out.println("  [CACHE HIT] key=" + key);
                return cache.get(key);
            }

            System.out.println("  [CACHE MISS] key=" + key + ", ttl=" + cached.ttlSeconds() + "s");
            Object result = pjp.proceed();
            cache.put(key, result);
            return result;
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // Configuration
    // ════════════════════════════════════════════════════════════════════════
    @Configuration
    @EnableAspectJAutoProxy  // bật AOP proxy
    @ComponentScan(basePackageClasses = AopDemo.class)
    static class AppConfig {}


    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }


    public static void main(String[] args) {
        System.out.println("═══════════════════════════════");
        System.out.println("  AOP DEMO");
        System.out.println("═══════════════════════════════");

        var ctx = new AnnotationConfigApplicationContext(AppConfig.class);

        ProductService productSvc = ctx.getBean(ProductService.class);
        OrderService   orderSvc   = ctx.getBean(OrderService.class);

        // ── 1. Normal call — logging + timing ─────────────────────────────────
        System.out.println("\n[1] Normal method call");
        String product = productSvc.getProduct(1L);
        System.out.println("  Result: " + product);

        // ── 2. Exception — AfterThrowing ──────────────────────────────────────
        System.out.println("\n[2] Exception case");
        try {
            productSvc.getProduct(-1L);
        } catch (IllegalArgumentException e) {
            System.out.println("  Caught by caller: " + e.getMessage());
        }

        // ── 3. Caching aspect ─────────────────────────────────────────────────
        System.out.println("\n[3] Caching aspect");
        System.out.println("  Call 1: " + productSvc.getProductWithCache(42L)); // cache miss
        System.out.println("  Call 2: " + productSvc.getProductWithCache(42L)); // cache hit
        System.out.println("  Call 3: " + productSvc.getProductWithCache(42L)); // cache hit

        // ── 4. Multiple method calls ──────────────────────────────────────────
        System.out.println("\n[4] Multiple calls");
        productSvc.updateProduct(1L, "New Laptop");
        orderSvc.placeOrder(1L, 5L, 2);

        // ── 5. Self-invocation — AOP KHÔNG hoạt động ─────────────────────────
        System.out.println("\n[5] Self-invocation gotcha");
        System.out.println("  Gọi processWithSelfInvocation() — @Timed trên placeOrder KHÔNG được apply:");
        orderSvc.processWithSelfInvocation();
        System.out.println("  ↑ Không thấy [PERF] log cho placeOrder bên trong — đó là lỗi!");

        // Fix: inject self hoặc tách thành service khác
        System.out.println("\n  Fix: gọi qua bean (qua proxy) → AOP hoạt động:");
        orderSvc.placeOrder(1L, 1L, 1); // gọi trực tiếp từ ngoài → có [PERF] log

        ctx.close();

        System.out.println("\n── Key Takeaways ──");
        System.out.println("  ✓ @Before:          chạy TRƯỚC method — logging input");
        System.out.println("  ✓ @AfterReturning:  chạy khi SUCCESS — logging output");
        System.out.println("  ✓ @AfterThrowing:   chạy khi EXCEPTION — error tracking");
        System.out.println("  ✓ @Around:          bao hết — có thể modify input/output/exception");
        System.out.println("  ✓ Custom annotation: @Timed, @Cached → Pointcut rõ ràng hơn");
        System.out.println("  ✗ Self-invocation:  this.method() không qua Proxy → AOP không apply");
        System.out.println("  ✗ Private method:   Proxy không override → AOP không apply");
        System.out.println("  ✓ @Transactional và @Cacheable của Spring hoạt động theo đúng cơ chế này");
    }
}
