package com.example.springclaudeturtorial.phase7;

import com.example.springclaudeturtorial.phase3.product.Product;
import com.example.springclaudeturtorial.phase3.product.ProductRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * ============================================================
 * PHASE 7 — Testcontainers: Test với Database thật
 * ============================================================
 *
 * Vấn đề với H2 in-memory (cách cũ):
 *   - H2 có SQL dialect khác PostgreSQL/SQL Server
 *   - Một số query chạy được trên H2 nhưng fail trên DB thật
 *   - Behavior của index, constraint, collation có thể khác
 *   → "Works on my machine" problem
 *
 * Testcontainers giải quyết bằng cách:
 *   - Chạy PostgreSQL THẬT trong Docker container
 *   - Container start trước test, stop sau test
 *   - Test chạy trên DB giống production nhất có thể
 *
 * Yêu cầu: Docker đang chạy trên máy.
 *
 * ── @Container lifecycle ──────────────────────────────────
 *   static → 1 container dùng chung cho toàn bộ test class
 *             (reuse container — nhanh hơn)
 *   non-static → mỗi test method tạo container mới
 *                (isolated — chậm hơn, hiếm khi cần)
 *
 * ── @DynamicPropertySource ───────────────────────────────
 *   PostgreSQL container bind port ngẫu nhiên mỗi lần start.
 *   @DynamicPropertySource override Spring datasource config
 *   bằng URL/credentials thực tế của container.
 * ============================================================
 */
@DataJpaTest
@Testcontainers
// Replace.NONE: KHÔNG dùng H2 in-memory tự động
// → dùng datasource từ @DynamicPropertySource (PostgreSQL container)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("ProductRepository — Testcontainers (Real PostgreSQL)")
class ProductRepositoryContainerTest {

    // ── Container definition ──────────────────────────────────────────────
    // static → tạo 1 lần, dùng chung cho toàn bộ test class
    // PostgreSQL 16 image từ Docker Hub
    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("testdb")
                    .withUsername("test")
                    .withPassword("test");

    // ── Inject container datasource vào Spring config ─────────────────────
    // Chạy trước khi ApplicationContext được tạo
    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",      postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name",
                     () -> "org.postgresql.Driver");
        // Hibernate dialect tự detect từ driver
        registry.add("spring.jpa.database-platform",
                     () -> "org.hibernate.dialect.PostgreSQLDialect");
    }

    @Autowired ProductRepository   productRepository;
    @Autowired TestEntityManager   em;

    @BeforeEach
    void setUp() {
        em.persist(new Product("Laptop",  "Electronics", 25_000_000.0, 10));
        em.persist(new Product("Monitor", "Electronics", 10_000_000.0,  5));
        em.persist(new Product("Chair",   "Furniture",    6_000_000.0,  8));
        em.persist(new Product("Phone",   "Electronics", 15_000_000.0,  0));
        em.flush();
    }

    @Test
    @DisplayName("Container phải đang chạy")
    void containerIsRunning() {
        assertThat(postgres.isRunning()).isTrue();
    }

    @Test
    @DisplayName("findByCategory trên PostgreSQL thật")
    void findByCategory_realPostgres_returnsCorrectResults() {
        List<Product> electronics = productRepository.findByCategory("Electronics");

        assertThat(electronics).hasSize(3);
        assertThat(electronics)
            .extracting(Product::getName)
            .containsExactlyInAnyOrder("Laptop", "Monitor", "Phone");
    }

    @Test
    @DisplayName("findByStockGreaterThan — PostgreSQL không trả về hàng hết stock")
    void findByStockGreaterThan_excludesZeroStock() {
        List<Product> inStock = productRepository.findByStockGreaterThan(0);

        assertThat(inStock).hasSize(3);
        assertThat(inStock)
            .extracting(Product::getName)
            .doesNotContain("Phone");   // Phone có stock = 0
    }

    @Test
    @DisplayName("findByPriceRange — kết quả đúng trên PostgreSQL")
    void findByPriceRange_returnsCorrectRange() {
        // Lấy sản phẩm giá 5M đến 20M
        List<Product> result = productRepository.findByPriceRange(5_000_000, 20_000_000);

        assertThat(result).hasSize(3);  // Monitor(10M), Chair(6M), Phone(15M)
        // Kiểm tra sort theo price ASC (nếu query có ORDER BY)
        assertThat(result)
            .extracting(Product::getPrice)
            .isSorted();
    }

    @Test
    @DisplayName("UNIQUE constraint thật — không cho phép tên trùng")
    void save_duplicateName_throwsConstraintViolation() {
        // PostgreSQL enforce UNIQUE constraint ở DB level
        // Khác H2 ở chỗ: behavior hoàn toàn giống production
        em.persist(new Product("UniqueProduct", "Electronics", 1_000_000.0, 1));
        em.flush();

        // Thêm product cùng tên → constraint violation
        assertThatThrownBy(() -> {
            em.persist(new Product("UniqueProduct", "Books", 2_000_000.0, 2));
            em.flush();
        }).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Transaction rollback sau mỗi test — DB sạch")
    void eachTest_startsWithFreshData() {
        // Mỗi test có @Transactional tự động (do @DataJpaTest)
        // → rollback sau test → không ảnh hưởng test khác
        long count = productRepository.count();
        assertThat(count).isEqualTo(4);  // chỉ có data từ setUp()
    }
}
