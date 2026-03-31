package com.example.springclaudeturtorial.phase3.product;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Seed dữ liệu mẫu khi chạy profile "local".
 * CommandLineRunner chạy SAU khi ApplicationContext khởi động xong.
 */
@Component
@Profile("local")
public class DataSeeder implements CommandLineRunner {

    private final ProductRepository productRepository;

    public DataSeeder(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) return; // idempotent

        productRepository.saveAll(java.util.List.of(
            new Product("Laptop Pro 15",   "Electronics", 25_000_000.0, 10),
            new Product("iPhone 16",       "Electronics", 28_000_000.0, 25),
            new Product("Mechanical Keyboard", "Electronics", 2_500_000.0, 50),
            new Product("4K Monitor",      "Electronics", 12_000_000.0, 15),
            new Product("Standing Desk",   "Furniture",    8_500_000.0,  5),
            new Product("Ergonomic Chair", "Furniture",    6_000_000.0,  8)
        ));

        System.out.println("[DataSeeder] Seeded " + productRepository.count() + " products");
    }
}
