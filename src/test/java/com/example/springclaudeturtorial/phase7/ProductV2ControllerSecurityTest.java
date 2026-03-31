package com.example.springclaudeturtorial.phase7;

import com.example.springclaudeturtorial.phase3.product.Product;
import com.example.springclaudeturtorial.phase3.product.ProductService;
import com.example.springclaudeturtorial.phase5.security.JwtAuthenticationFilter;
import com.example.springclaudeturtorial.phase5.security.JwtUtil;
import com.example.springclaudeturtorial.phase5.security.UserDetailsServiceImpl;
import com.example.springclaudeturtorial.phase5.web.ProductV2Controller;
import com.example.springclaudeturtorial.phase5.web.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * ============================================================
 * PHASE 7 — @WebMvcTest với Spring Security
 * ============================================================
 *
 * Đây là loại test quan trọng nhất khi có Security:
 * Kiểm tra cả logic controller VÀ quy tắc phân quyền.
 *
 * Chiến lược test security:
 *
 *   @WithMockUser             → user thường (ROLE_USER), đã xác thực
 *   @WithMockUser(roles="ADMIN") → admin user
 *   không có @WithMockUser    → anonymous (chưa đăng nhập) → 401/403
 *
 * Sự khác biệt 401 vs 403:
 *   401 Unauthorized  → chưa xác thực (anonymous user)
 *   403 Forbidden     → đã xác thực nhưng không đủ quyền
 * ============================================================
 */
@WebMvcTest(ProductV2Controller.class)
@DisplayName("ProductV2Controller — Security Tests")
class ProductV2ControllerSecurityTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    // Mock service — không load DB
    @MockBean ProductService productService;

    // Mock security infrastructure beans — SecurityConfig cần các bean này
    // Nếu không mock, Spring Boot Test không thể khởi tạo ApplicationContext
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean UserDetailsServiceImpl  userDetailsService;
    @MockBean JwtUtil                 jwtUtil;

    Product laptop;

    @BeforeEach
    void setUp() {
        laptop = new Product("Laptop", "Electronics", 25_000_000.0, 10);
    }

    // ══════════════════════════════════════════════════════════════════
    // PUBLIC ENDPOINTS — ai cũng truy cập được (không cần đăng nhập)
    // Theo SecurityConfig: GET /api/v2/products/** → permitAll()
    // ══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("GET /products — anonymous user → 200 (public endpoint)")
    void getAll_anonymousUser_returns200() throws Exception {
        // Không có @WithMockUser → anonymous
        Page<Product> page = new PageImpl<>(List.of(laptop));
        given(productService.findAllPaged(any(Pageable.class))).willReturn(page);

        mockMvc.perform(get("/api/v2/products"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /products/{id} — anonymous user → 200 (public)")
    void getById_anonymousUser_returns200() throws Exception {
        given(productService.findById(1L)).willReturn(laptop);

        mockMvc.perform(get("/api/v2/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("Laptop")));
    }

    // ══════════════════════════════════════════════════════════════════
    // AUTHENTICATED ENDPOINTS — cần đăng nhập
    // POST yêu cầu @WithMockUser (bất kỳ role nào)
    // ══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /products — anonymous → 401 Unauthorized")
    void create_anonymousUser_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(laptop);

        mockMvc.perform(post("/api/v2/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isUnauthorized());    // 401 — chưa đăng nhập
    }

    @Test
    @DisplayName("POST /products — ROLE_USER → 201 Created")
    @WithMockUser(username = "user1", roles = "USER")
    void create_withUserRole_returns201() throws Exception {
        given(productService.create(any(Product.class))).willReturn(laptop);

        String body = objectMapper.writeValueAsString(laptop);
        mockMvc.perform(post("/api/v2/products")
                .with(csrf())                         // CSRF token cho POST
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))   // Location header phải có
            .andExpect(jsonPath("$.name", is("Laptop")));
    }

    @Test
    @DisplayName("POST /products — validation fail → 400 với danh sách field errors")
    @WithMockUser
    void create_withInvalidBody_returns400WithFieldErrors() throws Exception {
        String invalidBody = """
            {
                "name": "",
                "category": "InvalidCategory",
                "price": -100
            }
            """;

        mockMvc.perform(post("/api/v2/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
            .andExpect(jsonPath("$.errors", hasSize(greaterThan(0))))  // có field errors
            .andExpect(jsonPath("$.errors[*].field",
                    hasItems("name", "category", "price")));           // đúng fields
    }

    // ══════════════════════════════════════════════════════════════════
    // ADMIN-ONLY ENDPOINTS — chỉ ROLE_ADMIN mới được
    // ══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("DELETE /products/{id} — ROLE_USER → 403 Forbidden")
    @WithMockUser(roles = "USER")
    void delete_withUserRole_returns403() throws Exception {
        // User có role USER → SecurityConfig từ chối DELETE → 403
        mockMvc.perform(delete("/api/v2/products/1").with(csrf()))
            .andExpect(status().isForbidden());    // 403 — đã đăng nhập nhưng không đủ quyền
    }

    @Test
    @DisplayName("DELETE /products/{id} — ROLE_ADMIN → 204 No Content")
    @WithMockUser(roles = "ADMIN")
    void delete_withAdminRole_returns204() throws Exception {
        willDoNothing().given(productService).delete(1L);

        mockMvc.perform(delete("/api/v2/products/1").with(csrf()))
            .andExpect(status().isNoContent());    // 204 — admin được phép xóa
    }

    @Test
    @DisplayName("DELETE /products/99 — ROLE_ADMIN, không tồn tại → 404")
    @WithMockUser(roles = "ADMIN")
    void delete_withAdminRole_notFound_returns404() throws Exception {
        willThrow(new ResourceNotFoundException("Product", 99L))
            .given(productService).delete(99L);

        mockMvc.perform(delete("/api/v2/products/99").with(csrf()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    // ══════════════════════════════════════════════════════════════════
    // @PreAuthorize method-level — kiểm tra annotation trên controller
    // ══════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("POST /products — @PreAuthorize('hasAnyRole') — ROLE_ADMIN cũng được phép")
    @WithMockUser(roles = "ADMIN")
    void create_withAdminRole_returns201() throws Exception {
        given(productService.create(any(Product.class))).willReturn(laptop);

        String body = objectMapper.writeValueAsString(laptop);
        mockMvc.perform(post("/api/v2/products")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated());
    }
}
