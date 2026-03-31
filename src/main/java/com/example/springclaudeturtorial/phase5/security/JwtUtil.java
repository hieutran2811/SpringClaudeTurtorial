package com.example.springclaudeturtorial.phase5.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * ============================================================
 * PHASE 5 — JWT Utility
 * ============================================================
 *
 * JWT (JSON Web Token) gồm 3 phần, ngăn cách bởi dấu ".":
 *   Header.Payload.Signature
 *
 *   Header  : thuật toán ký (HS256)
 *   Payload : claims — username, role, iat (issued at), exp (expiry)
 *   Signature: HMAC-SHA256(Header + Payload, secret_key)
 *
 * Server KHÔNG lưu token — chỉ verify chữ ký khi nhận request.
 * Đây là điểm khác biệt với Session (server lưu state).
 *
 * Secret key phải dài ≥ 256 bit (32 bytes) cho HS256.
 * ============================================================
 */
@Component
public class JwtUtil {

    private final SecretKey key;
    private final long      expirationMs;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms:3600000}") long expirationMs) {

        // Keys.hmacShaKeyFor đảm bảo key đủ mạnh cho HS256
        this.key          = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // ── Tạo token ──────────────────────────────────────────────────────────
    public String generateToken(String username, String role) {
        Date now    = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)              // "sub" claim
                .claim("role", role)            // custom claim
                .issuedAt(now)                  // "iat"
                .expiration(expiry)             // "exp"
                .signWith(key)
                .compact();
    }

    // ── Lấy username từ token ──────────────────────────────────────────────
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    // ── Lấy role từ token ─────────────────────────────────────────────────
    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    // ── Validate token (chữ ký + hạn dùng) ────────────────────────────────
    public boolean isValid(String token) {
        try {
            parseClaims(token);   // ném exception nếu invalid hoặc expired
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    // ── Parse (dùng nội bộ) ───────────────────────────────────────────────
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
