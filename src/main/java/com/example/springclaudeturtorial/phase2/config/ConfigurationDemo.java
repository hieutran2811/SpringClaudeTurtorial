package com.example.springclaudeturtorial.phase2.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * TOPIC: @Configuration, @Bean, @Profile, @Conditional
 *
 * Cách Spring quản lý cấu hình theo môi trường và điều kiện.
 */
public class ConfigurationDemo {

    // ── Interfaces ─────────────────────────────────────────────────────────
    interface EmailSender {
        void send(String to, String subject, String body);
    }

    interface CacheService {
        void put(String key, Object value);
        Object get(String key);
        String type();
    }

    interface FeatureFlag {
        boolean isEnabled(String feature);
    }


    // ── Implementations ────────────────────────────────────────────────────
    static class SmtpEmailSender implements EmailSender {
        private final String host;
        SmtpEmailSender(String host) { this.host = host; }
        public void send(String to, String subject, String body) {
            System.out.println("  [SMTP:" + host + "] → " + to + " | " + subject);
        }
    }

    static class MockEmailSender implements EmailSender {
        public void send(String to, String subject, String body) {
            System.out.println("  [MOCK EMAIL] → " + to + " | " + subject + " | " + body);
        }
    }

    static class RedisCacheService implements CacheService {
        private final java.util.Map<String, Object> store = new java.util.HashMap<>();
        public void put(String key, Object value) {
            store.put(key, value);
            System.out.println("  [Redis] PUT " + key + " = " + value);
        }
        public Object get(String key) {
            Object val = store.get(key);
            System.out.println("  [Redis] GET " + key + " = " + val);
            return val;
        }
        public String type() { return "Redis"; }
    }

    static class InMemoryCacheService implements CacheService {
        private final java.util.Map<String, Object> store = new java.util.HashMap<>();
        public void put(String key, Object value) {
            store.put(key, value);
            System.out.println("  [InMemory] PUT " + key + " = " + value);
        }
        public Object get(String key) {
            Object val = store.get(key);
            System.out.println("  [InMemory] GET " + key + " = " + val);
            return val;
        }
        public String type() { return "InMemory"; }
    }


    // ════════════════════════════════════════════════════════════════════════
    // Config cho môi trường LOCAL
    // ════════════════════════════════════════════════════════════════════════
    @Configuration
    @Profile("local")
    static class LocalConfig {

        @Bean
        public EmailSender emailSender() {
            System.out.println("  [Config] LOCAL — using MockEmailSender");
            return new MockEmailSender();
        }

        @Bean
        public CacheService cacheService() {
            System.out.println("  [Config] LOCAL — using InMemoryCacheService");
            return new InMemoryCacheService();
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // Config cho môi trường PRODUCTION
    // ════════════════════════════════════════════════════════════════════════
    @Configuration
    @Profile("production")
    static class ProductionConfig {

        @Bean
        public EmailSender emailSender() {
            System.out.println("  [Config] PRODUCTION — using SmtpEmailSender");
            return new SmtpEmailSender("smtp.sendgrid.net");
        }

        @Bean
        public CacheService cacheService() {
            System.out.println("  [Config] PRODUCTION — using RedisCacheService");
            return new RedisCacheService();
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // @ConditionalOnProperty — bật/tắt bean theo config property
    // ════════════════════════════════════════════════════════════════════════
    @Configuration
    static class FeatureConfig {

        // Bean chỉ tạo khi feature.analytics.enabled=true
        @Bean
        @ConditionalOnProperty(name = "feature.analytics.enabled", havingValue = "true")
        public FeatureFlag analyticsFeatureFlag() {
            System.out.println("  [Feature] Analytics ENABLED");
            return feature -> true;
        }

        // Bean chỉ tạo khi CHƯA có bean nào implement FeatureFlag
        @Bean
        @ConditionalOnMissingBean(FeatureFlag.class)
        public FeatureFlag defaultFeatureFlag() {
            System.out.println("  [Feature] Using default (disabled) feature flags");
            return feature -> false;
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // @Bean inter-dependency — bean A dùng bean B
    // ════════════════════════════════════════════════════════════════════════
    @Configuration
    static class ServiceConfig {

        // Spring đảm bảo emailSender() chỉ gọi 1 lần dù được reference nhiều chỗ
        // (vì @Configuration class được CGLIB proxy)
        @Bean
        public EmailSender emailSender(Environment env) {
            String profile = env.getActiveProfiles().length > 0
                ? env.getActiveProfiles()[0] : "default";
            if ("production".equals(profile)) {
                return new SmtpEmailSender("smtp.example.com");
            }
            return new MockEmailSender();
        }

        @Bean
        public NotificationService notificationService(EmailSender emailSender) {
            // inject emailSender bean — Spring tự resolve
            return new NotificationService(emailSender);
        }

        @Bean
        public UserService userService(NotificationService notificationService,
                                       CacheService cacheService) {
            return new UserService(notificationService, cacheService);
        }

        @Bean
        public CacheService cacheService() {
            return new InMemoryCacheService();
        }
    }


    // ── Service classes ────────────────────────────────────────────────────
    @Service
    static class NotificationService {
        private final EmailSender emailSender;

        NotificationService(EmailSender emailSender) {
            this.emailSender = emailSender;
        }

        public void notifyUser(String email, String message) {
            emailSender.send(email, "Notification", message);
        }
    }

    @Service
    static class UserService {
        private final NotificationService notificationService;
        private final CacheService cacheService;

        UserService(NotificationService notificationService, CacheService cacheService) {
            this.notificationService = notificationService;
            this.cacheService = cacheService;
        }

        public void registerUser(String email) {
            // Lưu vào cache
            cacheService.put("user:" + email, email);
            // Gửi thông báo
            notificationService.notifyUser(email, "Welcome to our platform!");
        }

        public void getUser(String email) {
            Object cached = cacheService.get("user:" + email);
            if (cached != null) {
                System.out.println("  [UserService] Found user in cache: " + cached);
            }
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // MAIN
    // ════════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════");
        System.out.println("  CONFIGURATION DEMO");
        System.out.println("═══════════════════════════════");

        // ── Chạy với profile "local" ──────────────────────────────────────────
        System.out.println("\n[A] Profile = local");
        var localCtx = new AnnotationConfigApplicationContext();
        localCtx.getEnvironment().setActiveProfiles("local");
        localCtx.register(LocalConfig.class, FeatureConfig.class, ServiceConfig.class);
        localCtx.refresh();

        EmailSender localEmail = localCtx.getBean(EmailSender.class);
        CacheService localCache = localCtx.getBean(CacheService.class);
        localEmail.send("test@example.com", "Test", "Hello");
        System.out.println("  Cache type: " + localCache.type());
        localCtx.close();

        // ── Chạy với profile "production" ────────────────────────────────────
        System.out.println("\n[B] Profile = production");
        var prodCtx = new AnnotationConfigApplicationContext();
        prodCtx.getEnvironment().setActiveProfiles("production");
        prodCtx.register(ProductionConfig.class, FeatureConfig.class, ServiceConfig.class);
        prodCtx.refresh();

        EmailSender prodEmail = prodCtx.getBean(EmailSender.class);
        CacheService prodCache = prodCtx.getBean(CacheService.class);
        prodEmail.send("user@example.com", "Welcome", "Hello from production");
        System.out.println("  Cache type: " + prodCache.type());
        prodCtx.close();

        // ── Bean inter-dependency ─────────────────────────────────────────────
        System.out.println("\n[C] Bean Dependencies (UserService → NotificationService → EmailSender)");
        var ctx = new AnnotationConfigApplicationContext(ServiceConfig.class);
        UserService userSvc = ctx.getBean(UserService.class);
        userSvc.registerUser("hieu@example.com");
        userSvc.getUser("hieu@example.com");
        ctx.close();

        System.out.println("\n── Key Takeaways ──");
        System.out.println("  ✓ @Profile: chọn config theo môi trường — không sửa code khi deploy");
        System.out.println("  ✓ @ConditionalOnProperty: bật/tắt bean theo feature flag trong config");
        System.out.println("  ✓ @ConditionalOnMissingBean: default bean nếu chưa có ai cung cấp");
        System.out.println("  ✓ @Configuration + CGLIB: đảm bảo @Bean method trả về cùng instance");
        System.out.println("  ✓ Bean dependency: khai báo param trong @Bean method, Spring tự inject");
    }
}
