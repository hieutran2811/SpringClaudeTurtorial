package com.example.springclaudeturtorial.phase3.autoconfig;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * TOPIC: Custom Auto-configuration
 *
 * Mô phỏng cách Spring Boot starters hoạt động bên trong.
 * Spring Boot đọc file META-INF/spring/...imports và tự load class này.
 *
 * Các @Conditional annotations:
 *  - @ConditionalOnMissingBean : chỉ tạo bean nếu app chưa define
 *  - @ConditionalOnProperty    : chỉ tạo bean nếu property = value
 *  - @ConditionalOnClass       : chỉ tạo bean nếu class tồn tại trong classpath
 */
@AutoConfiguration
@EnableConfigurationProperties(SmsProperties.class)
@ConditionalOnProperty(name = "sms.enabled", havingValue = "true", matchIfMissing = true)
public class SmsAutoConfiguration {

    // ── Twilio implementation ───────────────────────────────────────────────
    static class TwilioSmsSender implements SmsSender {
        private final String apiKey;
        TwilioSmsSender(String apiKey) { this.apiKey = apiKey; }
        public void send(String phone, String message) {
            System.out.println("  [Twilio] → " + phone + " | " + message + " (key=" + apiKey.substring(0, 4) + "...)");
        }
        public String provider() { return "Twilio"; }
    }

    // ── ESMS implementation ─────────────────────────────────────────────────
    static class EsmsSmsSender implements SmsSender {
        private final String apiKey;
        EsmsSmsSender(String apiKey) { this.apiKey = apiKey; }
        public void send(String phone, String message) {
            System.out.println("  [eSMS] → " + phone + " | " + message);
        }
        public String provider() { return "eSMS"; }
    }

    // ── Mock implementation (local/test) ────────────────────────────────────
    static class MockSmsSender implements SmsSender {
        public void send(String phone, String message) {
            System.out.println("  [MOCK SMS] → " + phone + " | " + message);
        }
        public String provider() { return "Mock"; }
    }

    /**
     * @ConditionalOnMissingBean: nếu app đã tự define SmsSender → dùng của app
     * Đây là cách Spring Boot cho phép override auto-configuration.
     */
    @Bean
    @ConditionalOnMissingBean(SmsSender.class)
    public SmsSender smsSender(SmsProperties props) {
        System.out.println("  [AutoConfig] Creating SmsSender for provider: " + props.provider());
        return switch (props.provider().toLowerCase()) {
            case "twilio" -> new TwilioSmsSender(props.apiKey());
            case "esms"   -> new EsmsSmsSender(props.apiKey());
            default       -> new MockSmsSender();
        };
    }
}
