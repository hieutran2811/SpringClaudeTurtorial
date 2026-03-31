package com.example.springclaudeturtorial.phase3.actuator;

import com.example.springclaudeturtorial.phase3.properties.AppProperties;
import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TOPIC: Custom Health Indicator
 *
 * Hiển thị tại: GET /actuator/health
 * Spring Boot tự gộp tất cả HealthIndicator vào response.
 */
@Component("paymentGateway")  // tên hiển thị trong /actuator/health
public class PaymentGatewayHealthIndicator implements HealthIndicator {

    private final AppProperties appProperties;

    public PaymentGatewayHealthIndicator(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public Health health() {
        Map<String, Object> details = new LinkedHashMap<>();

        try {
            // Giả lập ping đến payment gateways
            var results = checkGateways();
            long downCount = results.values().stream().filter(v -> !v).count();

            details.put("supportedGateways", appProperties.payment().supportedGateways());
            details.put("gatewayStatus", results);
            details.put("timeout", appProperties.payment().timeoutSeconds() + "s");

            if (downCount == 0) {
                return Health.up()
                    .withDetails(details)
                    .build();
            } else if (downCount < results.size()) {
                return Health.status("DEGRADED")   // custom status
                    .withDetail("downCount", downCount)
                    .withDetails(details)
                    .build();
            } else {
                return Health.down()
                    .withDetail("reason", "All gateways unreachable")
                    .withDetails(details)
                    .build();
            }
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .build();
        }
    }

    private Map<String, Boolean> checkGateways() {
        Map<String, Boolean> results = new LinkedHashMap<>();
        // Giả lập: vnpay UP, momo UP, zalopay DOWN
        for (String gw : appProperties.payment().supportedGateways()) {
            results.put(gw, !"zalopay".equals(gw)); // zalopay giả lập down
        }
        return results;
    }
}
