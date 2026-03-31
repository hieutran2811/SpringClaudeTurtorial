package com.example.springclaudeturtorial.phase5.security;

import jakarta.persistence.*;

/**
 * ============================================================
 * PHASE 5 — Spring Security: User Entity
 * ============================================================
 *
 * Đặt tên AppUser (không phải User) vì "USER" là reserved keyword
 * trong nhiều SQL databases, dễ gây lỗi Flyway migration.
 *
 * Role được lưu dạng String, ví dụ: "ROLE_USER", "ROLE_ADMIN"
 * Spring Security yêu cầu prefix "ROLE_" cho hasRole() hoạt động đúng.
 * ============================================================
 */
@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    private String password;       // lưu BCrypt hash, KHÔNG bao giờ plaintext

    @Column(nullable = false)
    private String role;           // "ROLE_USER" hoặc "ROLE_ADMIN"

    protected AppUser() {}

    public AppUser(String username, String password, String role) {
        this.username = username;
        this.password = password;
        this.role     = role;
    }

    public Long   getId()       { return id; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole()     { return role; }
}
