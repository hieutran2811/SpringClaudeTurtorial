package com.example.springclaudeturtorial.phase3.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * TOPIC: @ConfigurationProperties — Type-safe Configuration Binding
 *
 * Thay vì @Value("${app.payment.timeout}") rải rác khắp nơi,
 * gom toàn bộ config vào 1 class có type-safety + validation.
 *
 * prefix = "app" → bind tất cả property bắt đầu bằng "app."
 */
@ConfigurationProperties(prefix = "app")
@Validated  // bật Bean Validation trên properties
public record AppProperties(

    @NotBlank
    String name,

    @NotBlank
    String version,

    @Min(1) @Max(100)
    int maxConnections,

    @Valid Features features,
    @Valid Payment payment,
    @Valid Mail mail

) {
    // ── Nested config classes ─────────────────────────────────────────────

    public record Features(
        boolean newUiEnabled,
        boolean analyticsEnabled
    ) {}

    public record Payment(
        @Min(1) @Max(300)
        int timeoutSeconds,

        @Min(0) @Max(10)
        int retryMax,

        @NotEmpty
        List<String> supportedGateways
    ) {}

    public record Mail(
        @NotBlank @Email(regexp = ".*@.*\\..*")
        String from,

        String subjectPrefix
    ) {}
}
