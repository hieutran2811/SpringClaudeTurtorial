package com.example.springclaudeturtorial.phase3.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Đăng ký @ConfigurationProperties beans.
 * Cách 2: dùng @ConfigurationPropertiesScan ở main class (đơn giản hơn).
 */
@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class PropertiesConfig {}
