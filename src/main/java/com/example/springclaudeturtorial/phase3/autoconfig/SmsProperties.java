package com.example.springclaudeturtorial.phase3.autoconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config cho SMS auto-configuration.
 * Bind từ: sms.provider, sms.api-key, sms.enabled
 */
@ConfigurationProperties(prefix = "sms")
public record SmsProperties(
    String provider,   // "twilio" | "esms" | "mock"
    String apiKey,
    boolean enabled
) {
    public SmsProperties() {
        this("mock", "", true); // default values
    }
}
