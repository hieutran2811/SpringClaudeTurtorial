package com.example.springclaudeturtorial.phase3;

import com.example.springclaudeturtorial.phase3.product.*;
import com.example.springclaudeturtorial.phase5.web.exception.BusinessException;
import com.example.springclaudeturtorial.phase5.web.exception.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * LOẠI 1: Unit Test — test ProductService thuần túy
 *
 * @ExtendWith(MockitoExtension): KHÔNG load Spring context
 * Nhanh nhất (~ms), mock tất cả dependency bên ngoài.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService — Unit Tests")
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;   // Mock — không hit DB thật

    @InjectMocks
    ProductService productService;         // inject mock vào constructor


    // ── Fixtures ──────────────────────────────────────────────────────────
    Product laptop;
    Product phone;

    @BeforeEach
    void setUp() {
        laptop = new Product("Laptop", "Electronics", 25_000_000.0, 10);
        phone  = new Product("Phone",  "Electronics", 15_000_000.0, 20);
    }


    // ── findAll ────────────────────────────────────────────────────────────
    @Test
    @DisplayName("findAll() trả về danh sách products")
    void findAll_returnsAllProducts() {
        // Given
        given(productRepository.findAll()).willReturn(List.of(laptop, phone));

        // When
        List<Product> result = productService.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Product::getName)
            .containsExactlyInAnyOrder("Laptop", "Phone");

        then(productRepository).should(times(1)).findAll();
    }


    // ── findById ───────────────────────────────────────────────────────────
    @Test
    @DisplayName("findById() với id hợp lệ → trả về product")
    void findById_withValidId_returnsProduct() {
        given(productRepository.findById(1L)).willReturn(Optional.of(laptop));

        Product result = productService.findById(1L);

        assertThat(result.getName()).isEqualTo("Laptop");
        assertThat(result.getPrice()).isEqualTo(25_000_000.0);
    }

    @Test
    @DisplayName("findById() với id không tồn tại → throw ResourceNotFoundException")
    void findById_withInvalidId_throwsException() {
        given(productRepository.findById(99L)).willReturn(Optional.empty());

        // Phase 5: đổi từ NoSuchElementException → ResourceNotFoundException
        assertThatThrownBy(() -> productService.findById(99L))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("99");
    }


    // ── create ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("create() với tên mới → lưu và trả về product")
    void create_withNewName_savesProduct() {
        given(productRepository.existsByName("Laptop")).willReturn(false);
        given(productRepository.save(any(Product.class))).willReturn(laptop);

        Product result = productService.create(laptop);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Laptop");
        then(productRepository).should().save(laptop);
    }

    @Test
    @DisplayName("create() với tên đã tồn tại → throw BusinessException")
    void create_withDuplicateName_throwsException() {
        given(productRepository.existsByName("Laptop")).willReturn(true);

        // Phase 5: đổi từ IllegalArgumentException → BusinessException
        assertThatThrownBy(() -> productService.create(laptop))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Laptop");

        then(productRepository).should(never()).save(any());
    }


    // ── delete ─────────────────────────────────────────────────────────────
    @Test
    @DisplayName("delete() với id hợp lệ → gọi repository.delete()")
    void delete_withValidId_callsRepository() {
        given(productRepository.findById(1L)).willReturn(Optional.of(laptop));
        willDoNothing().given(productRepository).delete(laptop);

        productService.delete(1L);

        then(productRepository).should().delete(laptop);
    }


    // ── findByCategory ─────────────────────────────────────────────────────
    @Test
    @DisplayName("findByCategory() lọc đúng theo category")
    void findByCategory_returnsFilteredProducts() {
        given(productRepository.findByCategory("Electronics"))
            .willReturn(List.of(laptop, phone));

        List<Product> result = productService.findByCategory("Electronics");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(p -> "Electronics".equals(p.getCategory()));
    }
}
