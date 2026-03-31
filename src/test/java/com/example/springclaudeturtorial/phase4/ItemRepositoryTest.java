package com.example.springclaudeturtorial.phase4;

import com.example.springclaudeturtorial.phase4.jpa.domain.*;
import com.example.springclaudeturtorial.phase4.jpa.projection.ItemSummary;
import com.example.springclaudeturtorial.phase4.jpa.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.*;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("local")
@DisplayName("Phase 4 — JPA Repository Tests")
class ItemRepositoryTest {

    @Autowired ItemRepository     itemRepository;
    @Autowired CategoryRepository categoryRepository;

    Category electronics;
    Category furniture;
    Item     laptop;
    Item     keyboard;
    Item     outOfStock;
    Item     desk;

    @BeforeEach
    void setUp() {
        electronics = categoryRepository.save(new Category("Electronics", "Tech items"));
        furniture   = categoryRepository.save(new Category("Furniture", "Office items"));

        laptop      = new Item("Laptop",    new BigDecimal("25000000"), 10);
        keyboard    = new Item("Keyboard",  new BigDecimal("2500000"),  50);
        outOfStock  = new Item("Monitor",   new BigDecimal("12000000"),  0);
        desk        = new Item("Desk",      new BigDecimal("8500000"),   5);

        electronics.addItem(laptop);
        electronics.addItem(keyboard);
        electronics.addItem(outOfStock);
        furniture.addItem(desk);

        categoryRepository.saveAll(List.of(electronics, furniture));
    }

    // ── Derived queries ────────────────────────────────────────────────────
    @Test
    @DisplayName("findByCategoryName trả về đúng items")
    void findByCategoryName() {
        List<Item> items = itemRepository.findByCategoryName("Electronics");
        assertThat(items).hasSize(3);
        assertThat(items).extracting(Item::getName)
            .containsExactlyInAnyOrder("Laptop", "Keyboard", "Monitor");
    }

    @Test
    @DisplayName("findByStockGreaterThan(0) bỏ qua hết hàng")
    void findInStock() {
        List<Item> available = itemRepository.findByStockGreaterThan(0);
        assertThat(available).hasSize(3); // laptop + keyboard + desk
        assertThat(available).extracting(Item::getName).doesNotContain("Monitor");
    }

    // ── JPQL ──────────────────────────────────────────────────────────────
    @Test
    @DisplayName("findByPriceRange lọc đúng khoảng giá, sort ASC")
    void findByPriceRange() {
        List<Item> items = itemRepository.findByPriceRange(
            new BigDecimal("2000000"), new BigDecimal("15000000"));

        assertThat(items).hasSize(3); // keyboard(2.5M), desk(8.5M), outOfStock(12M)
        // Sorted by price ASC
        assertThat(items.get(0).getName()).isEqualTo("Keyboard");
        assertThat(items.get(1).getName()).isEqualTo("Desk");
    }

    @Test
    @DisplayName("searchByName tìm case-insensitive")
    void searchByName() {
        assertThat(itemRepository.searchByName("lap")).hasSize(1);
        assertThat(itemRepository.searchByName("LAP")).hasSize(1); // case-insensitive
        assertThat(itemRepository.searchByName("key")).hasSize(1);
        assertThat(itemRepository.searchByName("xxx")).isEmpty();
    }

    // ── Pagination ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("findAvailableItems pagination hoạt động đúng")
    void pagination() {
        Page<Item> page0 = itemRepository.findAvailableItems(
            PageRequest.of(0, 2, Sort.by("price").ascending())); // page 0, size 2

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(3); // laptop + keyboard + desk
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.isFirst()).isTrue();
        assertThat(page0.hasNext()).isTrue();

        Page<Item> page1 = itemRepository.findAvailableItems(
            PageRequest.of(1, 2, Sort.by("price").ascending())); // page 1
        assertThat(page1.getContent()).hasSize(1);
        assertThat(page1.isLast()).isTrue();
    }

    // ── Projection ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("Projection chỉ load id, name, price")
    void projection() {
        List<ItemSummary> summaries = itemRepository.findByCategory_Name("Electronics");

        assertThat(summaries).hasSize(3);
        // Verify projection methods work
        assertThat(summaries.get(0).getId()).isNotNull();
        assertThat(summaries.get(0).getName()).isNotBlank();
        assertThat(summaries.get(0).getPrice()).isPositive();
        assertThat(summaries.get(0).getDisplayPrice()).contains("đ");
    }

    // ── @Modifying ─────────────────────────────────────────────────────────
    @Test
    @DisplayName("decreaseStock giảm đúng số lượng")
    void decreaseStock_success() {
        int updated = itemRepository.decreaseStock(laptop.getId(), 3);
        assertThat(updated).isEqualTo(1);

        Item refreshed = itemRepository.findById(laptop.getId()).orElseThrow();
        assertThat(refreshed.getStock()).isEqualTo(7); // 10 - 3
    }

    @Test
    @DisplayName("decreaseStock không đủ stock → không update")
    void decreaseStock_insufficient() {
        int updated = itemRepository.decreaseStock(laptop.getId(), 999); // vượt quá stock
        assertThat(updated).isEqualTo(0); // WHERE stock >= qty không pass → không update

        Item refreshed = itemRepository.findById(laptop.getId()).orElseThrow();
        assertThat(refreshed.getStock()).isEqualTo(10); // không thay đổi
    }

    @Test
    @DisplayName("existsByNameIgnoreCase hoạt động đúng")
    void existsByName() {
        assertThat(itemRepository.existsByNameIgnoreCase("laptop")).isTrue();
        assertThat(itemRepository.existsByNameIgnoreCase("LAPTOP")).isTrue();
        assertThat(itemRepository.existsByNameIgnoreCase("Tablet")).isFalse();
    }
}
