package com.example.springclaudeturtorial.phase5.security.dto;

public record AuthResponse(
        String token,
        String tokenType,   // luôn là "Bearer"
        long   expiresIn    // số giây token còn hiệu lực
) {
    public static AuthResponse bearer(String token, long expiresIn) {
        return new AuthResponse(token, "Bearer", expiresIn);
    }
}
