package com.example.springclaudeturtorial.phase3;

import com.example.springclaudeturtorial.phase3.product.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * LOẠI 4: @SpringBootTest — Integration Test
 *
 * Load TOÀN BỘ ApplicationContext — gần giống production nhất.
 * Chậm nhất nhưng test realistic nhất.
 * Dùng H2 in-memory (profile local).
 *
 * @Transactional: rollback sau mỗi test → test độc lập nhau.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
@DisplayName("Product — Integration Tests")
// Phase 7: @WithMockUser ở class level → tất cả test chạy như user đã đăng nhập
// @SpringBootTest load full context kể cả SecurityConfig
@WithMockUser
class ProductIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired ProductRepository productRepository;


    @BeforeEach
    void setUp() {
        productRepository.saveAll(java.util.List.of(
            new Product("Laptop",  "Electronics", 25_000_000.0, 10),
            new Product("Monitor", "Electronics", 10_000_000.0,  5),
            new Product("Chair",   "Furniture",    6_000_000.0,  8)
        ));
    }


    // ── Full flow: GET all ─────────────────────────────────────────────────
    @Test
    @DisplayName("GET /products → trả về tất cả products từ DB thật")
    void getAll_returnsRealDataFromDB() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)));
    }


    // ── Full flow: POST → GET ──────────────────────────────────────────────
    @Test
    @DisplayName("POST /products → tạo mới → GET trả về đúng")
    void createThenGet_fullFlow() throws Exception {
        // Step 1: Tạo product
        Product newProduct = new Product("Headset", "Electronics", 3_000_000.0, 20);
        String responseBody = mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newProduct)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name", is("Headset")))
            .andReturn().getResponse().getContentAsString();

        // Step 2: Parse id từ response
        Product created = objectMapper.readValue(responseBody, Product.class);
        assertThat(created.getId()).isNotNull();

        // Step 3: GET bằng id vừa tạo
        mockMvc.perform(get("/api/v1/products/" + created.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name", is("Headset")))
            .andExpect(jsonPath("$.price", is(3_000_000.0)));
    }


    // ── Full flow: POST duplicate → 409 ───────────────────────────────────
    @Test
    @DisplayName("POST /products với tên đã tồn tại → 409 Conflict")
    void create_withDuplicateName_returns409() throws Exception {
        // Phase 5: BusinessException → 409 Conflict (không phải 400 nữa)
        Product duplicate = new Product("Laptop", "Electronics", 20_000_000.0, 5);

        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicate)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code", is("DUPLICATE_PRODUCT")));
    }


    // ── Full flow: DELETE → GET 404 ───────────────────────────────────────
    @Test
    @DisplayName("DELETE /products/{id} → GET lại trả về 404")
    void deleteThenGet_returns404() throws Exception {
        // Lấy id của Laptop
        Product laptop = productRepository.findAll().stream()
            .filter(p -> "Laptop".equals(p.getName()))
            .findFirst().orElseThrow();

        // Delete
        mockMvc.perform(delete("/api/v1/products/" + laptop.getId()))
            .andExpect(status().isNoContent());

        // Verify đã xoá
        mockMvc.perform(get("/api/v1/products/" + laptop.getId()))
            .andExpect(status().isNotFound());
    }


    // ── Filter by category ─────────────────────────────────────────────────
    @Test
    @DisplayName("GET /products?category=Furniture → chỉ lấy Furniture")
    void getByCategory_returnsOnlyMatchingCategory() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                .param("category", "Furniture"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].name", is("Chair")));
    }
}
