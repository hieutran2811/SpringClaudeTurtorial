package com.example.springclaudeturtorial.phase4.cache;

import com.example.springclaudeturtorial.phase4.jpa.domain.Item;
import com.example.springclaudeturtorial.phase4.jpa.repository.ItemRepository;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * TOPIC: Spring Cache Abstraction
 *
 * @Cacheable   — cache kết quả, lần sau trả từ cache (không gọi method)
 * @CachePut    — luôn gọi method VÀ update cache
 * @CacheEvict  — xoá entry khỏi cache
 * @Caching     — combine nhiều cache annotation
 */
@Service
@Transactional(readOnly = true)
public class CachedItemService {

    private final ItemRepository itemRepository;

    public CachedItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // ── @Cacheable ────────────────────────────────────────────────────────────
    /**
     * Cache key mặc định: method params → "items::1" (cacheName::key)
     * Lần đầu: gọi DB, lưu vào cache.
     * Lần sau: trả từ cache, KHÔNG gọi DB.
     */
    @Cacheable(cacheNames = CacheConfig.ITEMS, key = "#id")
    public Item findById(Long id) {
        System.out.println("  [DB] Loading item id=" + id);  // chỉ in khi cache miss
        return itemRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));
    }

    /**
     * Conditional cache: chỉ cache nếu item còn hàng.
     * condition: evaluate TRƯỚC khi gọi method (dựa trên params).
     * unless:    evaluate SAU khi gọi method (dựa trên result).
     */
    @Cacheable(
        cacheNames = CacheConfig.ITEMS,
        key        = "#id + '_available'",
        unless     = "#result.stock == 0"  // không cache nếu hết hàng
    )
    public Item findByIdIfAvailable(Long id) {
        System.out.println("  [DB] Loading available item id=" + id);
        return itemRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Item not found: " + id));
    }

    /**
     * Cache danh sách theo category.
     */
    @Cacheable(cacheNames = CacheConfig.ITEM_SUMMARY, key = "#categoryName")
    public List<Item> findByCategory(String categoryName) {
        System.out.println("  [DB] Loading items for category=" + categoryName);
        return itemRepository.findByCategoryName(categoryName);
    }


    // ── @CachePut ─────────────────────────────────────────────────────────────
    /**
     * Luôn gọi method (update DB) VÀ update cache.
     * Dùng khi: update entity → cache phải reflect giá trị mới ngay.
     */
    @Transactional
    @CachePut(cacheNames = CacheConfig.ITEMS, key = "#id")
    public Item updatePrice(Long id, BigDecimal newPrice) {
        System.out.println("  [DB] Updating price for id=" + id);
        Item item = itemRepository.findById(id).orElseThrow();
        item.setPrice(newPrice);
        return itemRepository.save(item);
    }


    // ── @CacheEvict ───────────────────────────────────────────────────────────
    /**
     * Xoá entry khỏi cache sau khi delete.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.ITEMS, key = "#id")
    public void deleteItem(Long id) {
        System.out.println("  [DB] Deleting item id=" + id);
        itemRepository.deleteById(id);
        // cache entry "items::id" bị xoá sau method này
    }

    /**
     * allEntries = true: xoá TOÀN BỘ cache "items".
     * Dùng khi bulk update làm invalidate nhiều entries.
     */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.ITEMS, allEntries = true)
    public void bulkUpdateStock(int delta) {
        System.out.println("  [DB] Bulk updating all stocks by " + delta);
        itemRepository.findAll().forEach(item -> {
            item.setStock(Math.max(0, item.getStock() + delta));
            itemRepository.save(item);
        });
    }


    // ── @Caching — kết hợp nhiều annotations ─────────────────────────────────
    /**
     * Update item: cập nhật cache "items", đồng thời evict "item-summary".
     */
    @Transactional
    @Caching(
        put    = { @CachePut(cacheNames  = CacheConfig.ITEMS, key = "#id") },
        evict  = { @CacheEvict(cacheNames = CacheConfig.ITEM_SUMMARY, allEntries = true) }
    )
    public Item updateItem(Long id, String newName, BigDecimal newPrice) {
        System.out.println("  [DB] Updating item id=" + id);
        Item item = itemRepository.findById(id).orElseThrow();
        item.setName(newName);
        item.setPrice(newPrice);
        return itemRepository.save(item);
    }


    // ── Cache-Aside Pattern (thủ công) ────────────────────────────────────────
    /**
     * Đôi khi cần kiểm soát cache thủ công.
     * Dùng khi logic cache phức tạp hơn annotation cho phép.
     */
    private final java.util.Map<String, List<Item>> manualCache = new java.util.HashMap<>();

    public List<Item> findByPriceRange_withManualCache(BigDecimal min, BigDecimal max) {
        String cacheKey = min + ":" + max;

        // Check cache
        if (manualCache.containsKey(cacheKey)) {
            System.out.println("  [ManualCache HIT] key=" + cacheKey);
            return manualCache.get(cacheKey);
        }

        // Cache miss → load from DB
        System.out.println("  [ManualCache MISS] key=" + cacheKey + " → loading from DB");
        List<Item> result = itemRepository.findByPriceRange(min, max);

        // Store in cache
        manualCache.put(cacheKey, result);
        return result;
    }
}
