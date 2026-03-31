package com.example.springclaudeturtorial.phase4.jpa.service;

import com.example.springclaudeturtorial.phase4.jpa.domain.*;
import com.example.springclaudeturtorial.phase4.jpa.projection.ItemSummary;
import com.example.springclaudeturtorial.phase4.jpa.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * TOPIC: Service Layer — JPA nâng cao
 *
 * Minh họa:
 * - Pagination & Sorting
 * - N+1 problem và cách fix bằng @EntityGraph
 * - Projection để giảm data transfer
 * - @Modifying queries
 */
@Service
@Transactional(readOnly = true)
public class ItemService {

    private final ItemRepository     itemRepository;
    private final CategoryRepository categoryRepository;

    public ItemService(ItemRepository itemRepository, CategoryRepository categoryRepository) {
        this.itemRepository     = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    // ── Pagination ────────────────────────────────────────────────────────────
    public Page<Item> findAvailableItems(int page, int size, String sortBy) {
        // Pageable: page (0-indexed), size, sort
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
        return itemRepository.findAvailableItems(pageable);
    }

    public Page<Item> findByCategory(String categoryName, int page, int size) {
        Pageable pageable = PageRequest.of(page, size,
            Sort.by("price").descending());     // sort price giảm dần
        return itemRepository.findByCategoryName(categoryName, pageable);
    }

    // ── N+1 Demo: BAD ─────────────────────────────────────────────────────────
    // Load 10 items → 1 query
    // Mỗi item.getCategory().getName() → thêm 1 query → tổng 11 queries!
    public List<String> getItemCategoryNames_BAD() {
        List<Item> items = itemRepository.findAll();  // query 1
        return items.stream()
            .map(i -> i.getName() + " → " + i.getCategory().getName()) // N queries!
            .toList();
    }

    // ── N+1 Fix: @EntityGraph JOIN FETCH ─────────────────────────────────────
    // 1 query duy nhất với JOIN FETCH items + categories + tags
    public List<String> getItemCategoryNames_GOOD() {
        List<Item> items = itemRepository.findAllWithCategoryAndTags(); // 1 query
        return items.stream()
            .map(i -> i.getName() + " → " + i.getCategory().getName())
            .toList();
    }

    // ── Projection ────────────────────────────────────────────────────────────
    public List<ItemSummary> getSummaryByCategory(String categoryName) {
        // Chỉ SELECT id, name, price — không load toàn entity
        return itemRepository.findByCategory_Name(categoryName);
    }

    // ── Search ────────────────────────────────────────────────────────────────
    public List<Item> searchByName(String keyword) {
        return itemRepository.searchByName(keyword);
    }

    public List<Item> findByPriceRange(BigDecimal min, BigDecimal max) {
        return itemRepository.findByPriceRange(min, max);
    }

    // ── Write operations ──────────────────────────────────────────────────────
    @Transactional
    public Item createItem(String categoryName, Item item) {
        Category category = categoryRepository.findByName(categoryName)
            .orElseThrow(() -> new NoSuchElementException("Category not found: " + categoryName));
        category.addItem(item); // helper method duy trì bidirectional sync
        return itemRepository.save(item);
    }

    @Transactional
    public int decreaseStock(Long itemId, int qty) {
        int updated = itemRepository.decreaseStock(itemId, qty);
        if (updated == 0) {
            throw new IllegalStateException(
                "Cannot decrease stock for item " + itemId + " by " + qty + " (insufficient stock)");
        }
        return updated;
    }

    @Transactional
    public int cleanupOutOfStock(String categoryName) {
        return itemRepository.deleteOutOfStockByCategory(categoryName);
    }

    public Item findById(Long id) {
        return itemRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));
    }
}
