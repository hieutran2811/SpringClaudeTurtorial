package com.example.springclaudeturtorial.phase4.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.*;

import java.util.List;

/**
 * TOPIC: Cache Configuration
 *
 * Local/test: ConcurrentMapCacheManager (in-memory)
 * Production: RedisCacheManager (uncomment khi có Redis)
 */
@Configuration
@EnableCaching  // bật @Cacheable, @CacheEvict, @CachePut
public class CacheConfig {

    // Cache names — dùng constants tránh typo
    public static final String ITEMS         = "items";
    public static final String ITEM_SUMMARY  = "item-summary";
    public static final String CATEGORIES    = "categories";

    /**
     * In-memory cache cho local/test.
     * Đổi sang RedisCacheManager khi có Redis:
     *
     * @Bean
     * public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
     *     RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
     *         .entryTtl(Duration.ofMinutes(10))
     *         .serializeValuesWith(
     *             RedisSerializationContext.SerializationPair.fromSerializer(
     *                 new GenericJackson2JsonRedisSerializer()));
     *
     *     return RedisCacheManager.builder(connectionFactory)
     *         .cacheDefaults(config)
     *         .withCacheConfiguration(ITEMS, config.entryTtl(Duration.ofMinutes(5)))
     *         .withCacheConfiguration(CATEGORIES, config.entryTtl(Duration.ofHours(1)))
     *         .build();
     * }
     */
    @Bean
    @Profile("!production")
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(ITEMS, ITEM_SUMMARY, CATEGORIES);
    }
}
