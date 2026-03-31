package com.example.springclaudeturtorial.phase3;

import com.example.springclaudeturtorial.phase3.product.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.*;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * LOẠI 3: @DataJpaTest — test Repository layer
 *
 * Chỉ load JPA layer: Entity, Repository, DataSource.
 * Dùng in-memory H2 database tự động (transaction rollback sau mỗi test).
 * KHÔNG load Controller, Service, Web layer.
 */
@DataJpaTest
@DisplayName("ProductRepository — Data Layer Tests")
class ProductRepositoryTest {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    TestEntityManager entityManager; // helper để setup test data

    Product laptop;
    Product chair;


    @BeforeEach
    void setUp() {
        laptop = entityManager.persist(
            new Product("Laptop", "Electronics", 25_000_000.0, 10));
        chair = entityManager.persist(
            new Product("Chair", "Furniture", 5_000_000.0, 8));
        entityManager.persist(
            new Product("Phone", "Electronics", 15_000_000.0, 0)); // stock = 0

        entityManager.flush(); // flush đến DB trước khi query
    }


    // ── findByCategory ─────────────────────────────────────────────────────
    @Test
    @DisplayName("findByCategory('Electronics') → trả về 2 products")
    void findByCategory_returnsMatchingProducts() {
        List<Product> result = productRepository.findByCategory("Electronics");

        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getCategory)
            .allMatch("Electronics"::equals);
    }

    @Test
    @DisplayName("findByCategory('NonExistent') → trả về list rỗng")
    void findByCategory_withNoMatch_returnsEmpty() {
        List<Product> result = productRepository.findByCategory("NonExistent");
        assertThat(result).isEmpty();
    }


    // ── findByStockGreaterThan ─────────────────────────────────────────────
    @Test
    @DisplayName("findByStockGreaterThan(0) → bỏ qua sản phẩm hết hàng")
    void findByStockGreaterThan_excludesOutOfStock() {
        List<Product> result = productRepository.findByStockGreaterThan(0);

        assertThat(result).hasSize(2); // Laptop(10) + Chair(8), bỏ Phone(0)
        assertThat(result).extracting(Product::getName)
            .doesNotContain("Phone");
    }


    // ── findByPriceRange ───────────────────────────────────────────────────
    @Test
    @DisplayName("findByPriceRange(4M, 20M) → lọc theo khoảng giá")
    void findByPriceRange_returnsProductsInRange() {
        List<Product> result = productRepository.findByPriceRange(4_000_000, 20_000_000);

        assertThat(result).hasSize(2);
        // Kết quả đã được sort theo price ASC
        assertThat(result.get(0).getName()).isEqualTo("Chair");   // 5M
        assertThat(result.get(1).getName()).isEqualTo("Phone");   // 15M
    }


    // ── existsByName ───────────────────────────────────────────────────────
    @Test
    @DisplayName("existsByName('Laptop') → true")
    void existsByName_withExistingName_returnsTrue() {
        assertThat(productRepository.existsByName("Laptop")).isTrue();
    }

    @Test
    @DisplayName("existsByName('Tablet') → false")
    void existsByName_withNonExistingName_returnsFalse() {
        assertThat(productRepository.existsByName("Tablet")).isFalse();
    }


    // ── save + findById ────────────────────────────────────────────────────
    @Test
    @DisplayName("save() → persist và generate id")
    void save_persistsEntityWithGeneratedId() {
        Product monitor = new Product("Monitor", "Electronics", 10_000_000.0, 5);
        Product saved = productRepository.save(monitor);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isPositive();
    }

    @Test
    @DisplayName("delete() → không còn trong DB")
    void delete_removesEntity() {
        productRepository.delete(laptop);
        entityManager.flush();

        assertThat(productRepository.existsByName("Laptop")).isFalse();
    }
}
