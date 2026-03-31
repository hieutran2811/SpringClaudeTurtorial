package com.example.springclaudeturtorial.phase5.security;

import com.example.springclaudeturtorial.phase5.security.dto.AuthRequest;
import com.example.springclaudeturtorial.phase5.security.dto.AuthResponse;
import com.example.springclaudeturtorial.phase5.web.exception.BusinessException;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * ============================================================
 * PHASE 5 — Authentication Controller
 * ============================================================
 *
 * POST /api/auth/register  → tạo tài khoản mới
 * POST /api/auth/login     → xác thực, trả về JWT
 *
 * Flow login:
 *   1. Client gửi username + password
 *   2. AuthenticationManager xác thực (dùng UserDetailsServiceImpl + BCrypt)
 *   3. Nếu đúng → JwtUtil tạo token → trả về client
 *   4. Client lưu token, gửi kèm mọi request sau:
 *      Authorization: Bearer <token>
 * ============================================================
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserRepository        userRepository;
    private final PasswordEncoder       passwordEncoder;
    private final JwtUtil               jwtUtil;

    public AuthController(AuthenticationManager authManager,
                          UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtUtil jwtUtil) {
        this.authManager     = authManager;
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil         = jwtUtil;
    }

    // ── POST /api/auth/register ───────────────────────────────────────────
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody AuthRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new BusinessException("DUPLICATE_USER",
                    "Username already taken: " + request.username());
        }

        AppUser newUser = new AppUser(
                request.username(),
                passwordEncoder.encode(request.password()),  // BCrypt hash
                "ROLE_USER"                                   // default role
        );
        userRepository.save(newUser);

        return ResponseEntity.ok("User registered successfully");
    }

    // ── POST /api/auth/login ──────────────────────────────────────────────
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        try {
            // AuthenticationManager kiểm tra username/password qua UserDetailsServiceImpl
            var authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.username(),
                            request.password()
                    )
            );

            // Lấy role từ GrantedAuthority đầu tiên
            String role = authentication.getAuthorities()
                    .iterator().next()
                    .getAuthority();

            String token = jwtUtil.generateToken(request.username(), role);

            return ResponseEntity.ok(
                    AuthResponse.bearer(token, jwtUtil.getExpirationSeconds()));

        } catch (AuthenticationException e) {
            // Trả về 401 — không tiết lộ username hay password sai cái nào
            return ResponseEntity.status(401).build();
        }
    }
}
