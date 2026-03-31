package com.example.springclaudeturtorial.phase3;

import com.example.springclaudeturtorial.phase3.product.*;
import com.example.springclaudeturtorial.phase5.security.JwtAuthenticationFilter;
import com.example.springclaudeturtorial.phase5.security.JwtUtil;
import com.example.springclaudeturtorial.phase5.security.UserDetailsServiceImpl;
import com.example.springclaudeturtorial.phase5.web.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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
 * LOẠI 2: @WebMvcTest — test Controller layer
 *
 * Chỉ load Web layer (Controller, Filter, ExceptionHandler).
 * KHÔNG load Service, Repository, Database.
 * Service được mock bằng @MockBean.
 *
 * Nhanh hơn @SpringBootTest vì không load full context.
 */
@WebMvcTest(ProductController.class)
@DisplayName("ProductController — Web Layer Tests")
// Phase 7: @WithMockUser ở class level → tất cả test chạy như user đã đăng nhập
// Không cần JWT token thật — Spring Security Test tự inject authentication vào SecurityContext
@WithMockUser
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ProductService productService;

    // Phase 7: mock các security beans để @WebMvcTest không fail khi load SecurityConfig
    // SecurityConfig cần 3 beans này — mock để Spring khởi tạo được context
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;
    @MockBean UserDetailsServiceImpl  userDetailsService;
    @MockBean JwtUtil                 jwtUtil;

    Product laptop;
    Product phone;

    @BeforeEach
    void setUp() {
        laptop = new Product("Laptop", "Electronics", 25_000_000.0, 10);
        phone  = new Product("Phone",  "Electronics", 15_000_000.0, 20);
    }


    // ── GET /api/v1/products ───────────────────────────────────────────────
    @Test
    @DisplayName("GET /products → 200 + danh sách products")
    void getAll_returns200WithProducts() throws Exception {
        given(productService.findAll()).willReturn(List.of(laptop, phone));

        mockMvc.perform(get("/api/v1/products")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[0].name", is("Laptop")))
            .andExpect(jsonPath("$[0].price", is(25_000_000.0)))
            .andExpect(jsonPath("$[1].name", is("Phone")));
    }


    // ── GET /api/v1/products/{id} ──────────────────────────────────────────
    @Test
    @DisplayName("GET /products/1 → 200 + product")
    void getById_withValidId_returns200() throws Exception {
        given(productService.findById(1L)).willReturn(laptop);

        mockMvc.perform(get("/api/v1/products/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("Laptop")))
            .andExpect(jsonPath("$.category", is("Electronics")));
    }

    @Test
    @DisplayName("GET /products/99 → 404 Not Found")
    void getById_withInvalidId_returns404() throws Exception {
        // Phase 5+7: mock throw ResourceNotFoundException (không phải NoSuchElementException)
        // GlobalExceptionHandler map ResourceNotFoundException → 404 với code "RESOURCE_NOT_FOUND"
        given(productService.findById(99L))
            .willThrow(new ResourceNotFoundException("Product", 99L));

        mockMvc.perform(get("/api/v1/products/99"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")))
            .andExpect(jsonPath("$.message", containsString("99")));
    }


    // ── POST /api/v1/products ──────────────────────────────────────────────
    @Test
    @DisplayName("POST /products với body hợp lệ → 201 Created")
    void create_withValidBody_returns201() throws Exception {
        Product newProduct = new Product("Headset", "Electronics", 3_000_000.0, 30);
        given(productService.create(any(Product.class))).willReturn(newProduct);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", is("Headset")));
    }

    @Test
    @DisplayName("POST /products với body thiếu name → 400 Bad Request (Bean Validation)")
    void create_withMissingName_returns400() throws Exception {
        // name = blank → vi phạm @NotBlank
        String invalidBody = """
            {
                "name": "",
                "category": "Electronics",
                "price": 1000000,
                "stock": 5
            }
            """;

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /products với price âm → 400 Bad Request")
    void create_withNegativePrice_returns400() throws Exception {
        String invalidBody = """
            {
                "name": "Product",
                "category": "Electronics",
                "price": -100,
                "stock": 5
            }
            """;

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidBody))
            .andExpect(status().isBadRequest());
    }


    // ── DELETE /api/v1/products/{id} ───────────────────────────────────────
    @Test
    @DisplayName("DELETE /products/1 → 204 No Content")
    void delete_withValidId_returns204() throws Exception {
        willDoNothing().given(productService).delete(1L);

        mockMvc.perform(delete("/api/v1/products/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /products/99 → 404 Not Found")
    void delete_withInvalidId_returns404() throws Exception {
        willThrow(new NoSuchElementException("Product not found: 99"))
            .given(productService).delete(99L);

        mockMvc.perform(delete("/api/v1/products/99"))
            .andExpect(status().isNotFound());
    }


    // ── GET với query param ────────────────────────────────────────────────
    @Test
    @DisplayName("GET /products?category=Electronics → filter theo category")
    void getByCategory_returnsFilteredList() throws Exception {
        given(productService.findByCategory("Electronics"))
            .willReturn(List.of(laptop, phone));

        mockMvc.perform(get("/api/v1/products")
                .param("category", "Electronics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)));
    }
}
