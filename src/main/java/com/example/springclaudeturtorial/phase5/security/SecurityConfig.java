package com.example.springclaudeturtorial.phase5.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * ============================================================
 * PHASE 5 — Spring Security Configuration
 * ============================================================
 *
 * Cấu trúc Security trong Spring Boot 3+/4+:
 *   - Không dùng WebSecurityConfigurerAdapter (deprecated)
 *   - Khai báo SecurityFilterChain @Bean thay thế
 *
 * @EnableWebSecurity     : kích hoạt Spring Security
 * @EnableMethodSecurity  : cho phép dùng @PreAuthorize, @PostAuthorize
 *                          trên method level (role-based authorization)
 *
 * Chiến lược STATELESS:
 *   - Server không lưu session
 *   - Mỗi request phải mang JWT
 *   - Phù hợp cho REST API và microservices
 * ============================================================
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity          // bật @PreAuthorize / @PostAuthorize
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final UserDetailsServiceImpl  userDetailsService;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter,
                          UserDetailsServiceImpl userDetailsService) {
        this.jwtFilter          = jwtFilter;
        this.userDetailsService = userDetailsService;
    }

    // ── Password encoder ──────────────────────────────────────────────────
    // BCrypt: hash một chiều với salt ngẫu nhiên — không thể reverse
    // Cost factor mặc định = 10 → ~100ms/hash, đủ chậm để chống brute force
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ── AuthenticationManager — dùng cho login ────────────────────────────
    @Bean
    public AuthenticationManager authenticationManager() {
        var provider = new org.springframework.security.authentication
                .dao.DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(provider);
    }

    // ── Security Filter Chain — trung tâm cấu hình ───────────────────────
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── 1. Tắt CSRF: không cần thiết với stateless JWT API ─────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── 2. Session: STATELESS — không tạo/dùng HTTP session ────────
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── 3. Authorization rules (đọc từ trên xuống, rule đầu tiên khớp thắng)
            .authorizeHttpRequests(auth -> auth

                // Public: đăng ký / đăng nhập — ai cũng gọi được
                .requestMatchers("/api/auth/**").permitAll()

                // Public: GET products — khách vãng lai xem được
                .requestMatchers(HttpMethod.GET, "/api/v2/products/**").permitAll()

                // H2 console (chỉ dùng local dev)
                .requestMatchers("/h2-console/**").permitAll()

                // Actuator health — public
                .requestMatchers("/actuator/health").permitAll()

                // Admin only: xóa product
                .requestMatchers(HttpMethod.DELETE, "/api/v2/products/**")
                    .hasRole("ADMIN")           // hasRole("ADMIN") = hasAuthority("ROLE_ADMIN")

                // Còn lại: phải đăng nhập
                .anyRequest().authenticated()
            )

            // ── 4. H2 console cần iframe → tắt X-Frame-Options ─────────────
            .headers(h -> h.frameOptions(fo -> fo.sameOrigin()))

            // ── 5. Thêm JWT filter TRƯỚC UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
