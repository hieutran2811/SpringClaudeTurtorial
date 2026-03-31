package com.example.springclaudeturtorial.phase3.actuator;

import com.example.springclaudeturtorial.phase3.properties.AppProperties;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * TOPIC: Custom InfoContributor
 *
 * Hiển thị tại: GET /actuator/info
 * Cung cấp thông tin app khi query.
 */
@Component
public class AppInfoContributor implements InfoContributor {

    private final AppProperties appProperties;

    public AppInfoContributor(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", Map.of(
            "name",       appProperties.name(),
            "version",    appProperties.version(),
            "startedAt",  LocalDateTime.now().toString()
        ));
        builder.withDetail("features", Map.of(
            "newUi",      appProperties.features().newUiEnabled(),
            "analytics",  appProperties.features().analyticsEnabled()
        ));
        builder.withDetail("runtime", Map.of(
            "java",       System.getProperty("java.version"),
            "os",         System.getProperty("os.name"),
            "cpus",       Runtime.getRuntime().availableProcessors(),
            "memoryMb",   Runtime.getRuntime().maxMemory() / 1024 / 1024
        ));
    }
}
