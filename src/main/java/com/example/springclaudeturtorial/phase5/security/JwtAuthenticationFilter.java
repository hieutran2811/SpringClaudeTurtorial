package com.example.springclaudeturtorial.phase5.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * ============================================================
 * PHASE 5 — JWT Authentication Filter
 * ============================================================
 *
 * OncePerRequestFilter: Spring đảm bảo filter này chạy ĐÚNG MỘT LẦN
 * mỗi request, tránh bị gọi nhiều lần trong filter chain.
 *
 * Luồng xử lý mỗi request:
 *
 *   Request
 *     → [JwtAuthenticationFilter]        ← filter này
 *         → đọc header Authorization: Bearer <token>
 *         → validate token bằng JwtUtil
 *         → set Authentication vào SecurityContext
 *     → [Spring Security authorization]
 *         → kiểm tra SecurityContext có quyền không?
 *     → [Controller]
 *
 * SecurityContextHolder: thread-local holder lưu thông tin người dùng
 * đã xác thực cho request hiện tại. Bị clear tự động sau request.
 * ============================================================
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Bỏ qua nếu không có header hoặc không bắt đầu bằng "Bearer "
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);   // bỏ "Bearer " (7 ký tự)

        if (jwtUtil.isValid(token)) {
            String username = jwtUtil.extractUsername(token);
            String role     = jwtUtil.extractRole(token);

            // Tạo Authentication object và đặt vào SecurityContext
            // credentials = null vì JWT stateless — không cần password sau khi verify
            var auth = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    List.of(new SimpleGrantedAuthority(role))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}
