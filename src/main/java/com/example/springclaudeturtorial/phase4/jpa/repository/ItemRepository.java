package com.example.springclaudeturtorial.phase4.jpa.repository;

import com.example.springclaudeturtorial.phase4.jpa.domain.Item;
import com.example.springclaudeturtorial.phase4.jpa.projection.ItemSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // ── 1. Derived Query Methods — Spring tự tạo SQL từ tên method ───────────
    List<Item>    findByStockGreaterThan(int stock);
    List<Item>    findByCategoryName(String categoryName);         // traverse relationship
    long          countByCategoryName(String categoryName);
    boolean       existsByNameIgnoreCase(String name);
    Optional<Item> findFirstByOrderByPriceDesc();                  // sản phẩm đắt nhất

    // ── 2. @Query JPQL — viết query rõ ràng hơn derived method ──────────────
    @Query("SELECT i FROM Item i WHERE i.price BETWEEN :min AND :max ORDER BY i.price ASC")
    List<Item> findByPriceRange(
        @Param("min") BigDecimal min,
        @Param("max") BigDecimal max
    );

    @Query("SELECT i FROM Item i WHERE LOWER(i.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Item> searchByName(@Param("keyword") String keyword);

    // ── 3. @EntityGraph — FIX N+1 problem ───────────────────────────────────
    // Không có @EntityGraph: load 10 items → 10 query load Category (N+1!)
    // Có @EntityGraph:       1 JOIN FETCH query load items + categories
    @EntityGraph(attributePaths = {"category", "tags"})
    @Query("SELECT i FROM Item i")
    List<Item> findAllWithCategoryAndTags();

    // ── 4. Projection — chỉ lấy field cần thiết, không load toàn entity ──────
    List<ItemSummary> findByCategory_Name(String categoryName);

    // ── 5. Pagination ─────────────────────────────────────────────────────────
    Page<Item> findByCategoryName(String categoryName, Pageable pageable);

    @Query("SELECT i FROM Item i WHERE i.stock > 0")
    Page<Item> findAvailableItems(Pageable pageable);

    // ── 6. @Modifying — UPDATE / DELETE query ────────────────────────────────
    @Modifying
    @Query("UPDATE Item i SET i.stock = i.stock - :qty WHERE i.id = :id AND i.stock >= :qty")
    int decreaseStock(@Param("id") Long id, @Param("qty") int qty);

    @Modifying
    @Query("DELETE FROM Item i WHERE i.stock = 0 AND i.category.name = :categoryName")
    int deleteOutOfStockByCategory(@Param("categoryName") String categoryName);
}
