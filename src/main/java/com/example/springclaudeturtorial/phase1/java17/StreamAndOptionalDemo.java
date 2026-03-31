package com.example.springclaudeturtorial.phase1.java17;

import java.util.*;
import java.util.stream.*;

/**
 * TOPIC: Stream API + Optional
 *
 * Stream: xử lý collection theo phong cách declarative (what, not how).
 * Optional: wrapper tránh NullPointerException, buộc caller xử lý null.
 */
public class StreamAndOptionalDemo {

    record Product(String name, String category, double price, int stock) {}
    record Order(String orderId, String customer, List<Product> items) {}

    // Sample data
    static List<Product> products() {
        return List.of(
            new Product("Laptop",   "Electronics", 25_000_000, 10),
            new Product("Phone",    "Electronics", 15_000_000, 25),
            new Product("Keyboard", "Electronics",  2_000_000,  5),
            new Product("Desk",     "Furniture",    8_000_000, 3),
            new Product("Chair",    "Furniture",    5_000_000, 8),
            new Product("Monitor",  "Electronics", 10_000_000, 0),  // out of stock
            new Product("Headset",  "Electronics",  3_000_000, 15)
        );
    }


    // ── A. Stream Operations ──────────────────────────────────────────────────

    static void streamBasics() {
        List<Product> all = products();

        // filter + map + collect
        List<String> expensiveNames = all.stream()
            .filter(p -> p.price() > 5_000_000)
            .filter(p -> p.stock() > 0)             // chỉ còn hàng
            .map(Product::name)                      // method reference
            .sorted()
            .collect(Collectors.toList());

        System.out.println("\n[A1] Expensive in-stock products: " + expensiveNames);

        // reduce — tính tổng giá trị tồn kho
        double totalValue = all.stream()
            .mapToDouble(p -> p.price() * p.stock())
            .sum();
        System.out.printf("[A2] Total inventory value: %,.0f đ%n", totalValue);

        // findFirst — tìm sản phẩm đầu tiên của category
        Optional<Product> firstElectronics = all.stream()
            .filter(p -> "Electronics".equals(p.category()))
            .findFirst();
        firstElectronics.ifPresent(p -> System.out.println("[A3] First Electronics: " + p.name()));

        // anyMatch / allMatch / noneMatch
        boolean hasOutOfStock  = all.stream().anyMatch(p -> p.stock() == 0);
        boolean allHavePrice   = all.stream().allMatch(p -> p.price() > 0);
        boolean noneNegStock   = all.stream().noneMatch(p -> p.stock() < 0);
        System.out.println("[A4] Has out-of-stock: " + hasOutOfStock
            + ", allHavePrice: " + allHavePrice
            + ", noneNegStock: " + noneNegStock);

        // count
        long electronicsCount = all.stream()
            .filter(p -> "Electronics".equals(p.category()))
            .count();
        System.out.println("[A5] Electronics count: " + electronicsCount);
    }


    // ── B. Collectors nâng cao ────────────────────────────────────────────────

    static void advancedCollectors() {
        List<Product> all = products();

        // groupingBy — nhóm theo category
        Map<String, List<Product>> byCategory = all.stream()
            .collect(Collectors.groupingBy(Product::category));

        System.out.println("\n[B1] Products by category:");
        byCategory.forEach((cat, items) -> {
            String names = items.stream().map(Product::name).collect(Collectors.joining(", "));
            System.out.println("    " + cat + ": " + names);
        });

        // groupingBy + counting
        Map<String, Long> countByCategory = all.stream()
            .collect(Collectors.groupingBy(Product::category, Collectors.counting()));
        System.out.println("[B2] Count by category: " + countByCategory);

        // groupingBy + averagingDouble
        Map<String, Double> avgPriceByCategory = all.stream()
            .collect(Collectors.groupingBy(
                Product::category,
                Collectors.averagingDouble(Product::price)
            ));
        System.out.println("[B3] Avg price by category:");
        avgPriceByCategory.forEach((cat, avg) ->
            System.out.printf("    %-15s %.0f đ%n", cat, avg));

        // partitioningBy — chia 2 nhóm (còn hàng / hết hàng)
        Map<Boolean, List<Product>> stockPartition = all.stream()
            .collect(Collectors.partitioningBy(p -> p.stock() > 0));
        System.out.println("[B4] In stock:      " + stockPartition.get(true).stream().map(Product::name).toList());
        System.out.println("[B5] Out of stock:  " + stockPartition.get(false).stream().map(Product::name).toList());

        // toMap — chuyển thành Map<name, price>
        Map<String, Double> priceMap = all.stream()
            .collect(Collectors.toMap(Product::name, Product::price));
        System.out.println("[B6] Price of Laptop: " + priceMap.get("Laptop"));

        // joining — ghép string
        String allNames = all.stream()
            .map(Product::name)
            .collect(Collectors.joining(", ", "[", "]"));
        System.out.println("[B7] All products: " + allNames);
    }


    // ── C. flatMap — làm phẳng nested collections ────────────────────────────

    static void flatMapDemo() {
        List<Order> orders = List.of(
            new Order("ORD-1", "Hieu", List.of(
                new Product("Laptop", "Electronics", 25_000_000, 1),
                new Product("Mouse",  "Electronics",    500_000, 1)
            )),
            new Order("ORD-2", "An", List.of(
                new Product("Chair",  "Furniture",    5_000_000, 2),
                new Product("Laptop", "Electronics", 25_000_000, 1)
            ))
        );

        // flatMap: List<Order> → stream tất cả Product
        List<String> allOrderedProducts = orders.stream()
            .flatMap(order -> order.items().stream())   // mở rộng nested list
            .map(Product::name)
            .distinct()
            .sorted()
            .toList();  // Java 16+ shorthand cho collect(Collectors.toUnmodifiableList())

        System.out.println("\n[C1] All ordered products: " + allOrderedProducts);

        // Tính tổng doanh thu từ tất cả orders
        double totalRevenue = orders.stream()
            .flatMap(o -> o.items().stream())
            .mapToDouble(p -> p.price() * p.stock())
            .sum();
        System.out.printf("[C2] Total revenue: %,.0f đ%n", totalRevenue);
    }


    // ── D. Optional ───────────────────────────────────────────────────────────

    static Optional<Product> findProduct(String name) {
        return products().stream()
            .filter(p -> p.name().equalsIgnoreCase(name))
            .findFirst();
    }

    static void optionalDemo() {
        System.out.println("\n[D] Optional Demo");

        // BAD — get() không an toàn
        // Optional<Product> p = findProduct("Laptop");
        // p.get(); // NullPointerException nếu empty!

        // GOOD patterns:

        // orElse — giá trị mặc định
        Product product = findProduct("Tablet")
            .orElse(new Product("Default", "Unknown", 0, 0));
        System.out.println("  orElse:          " + product.name());

        // orElseGet — lazy evaluation (chỉ gọi supplier khi empty)
        Product p2 = findProduct("Tablet")
            .orElseGet(() -> new Product("Generated", "Unknown", 0, 0));
        System.out.println("  orElseGet:       " + p2.name());

        // orElseThrow — throw exception khi không tìm thấy (service layer dùng nhiều)
        try {
            Product p3 = findProduct("Tablet")
                .orElseThrow(() -> new NoSuchElementException("Product 'Tablet' not found"));
        } catch (NoSuchElementException e) {
            System.out.println("  orElseThrow:     " + e.getMessage());
        }

        // ifPresent — chỉ chạy khi có giá trị
        findProduct("Laptop").ifPresent(p -> System.out.println("  ifPresent:       Found " + p.name()));

        // ifPresentOrElse — Java 9+
        findProduct("Tablet").ifPresentOrElse(
            p -> System.out.println("  Found: " + p.name()),
            () -> System.out.println("  ifPresentOrElse: Tablet not found")
        );

        // map + filter — chain transformations
        String priceDisplay = findProduct("Phone")
            .filter(p -> p.stock() > 0)
            .map(p -> String.format("%s: %,.0f đ", p.name(), p.price()))
            .orElse("Not available");
        System.out.println("  map+filter:      " + priceDisplay);

        // flatMap — khi transform trả về Optional
        Optional<String> category = findProduct("Monitor")
            .flatMap(p -> p.stock() > 0 ? Optional.of(p.category()) : Optional.empty());
        System.out.println("  flatMap:         " + category.orElse("Out of stock — no category"));

        // Optional.ofNullable — wrap giá trị có thể null
        String nullableName = null;
        String safeName = Optional.ofNullable(nullableName).orElse("anonymous");
        System.out.println("  ofNullable:      " + safeName);
    }


    public static void main(String[] args) {
        System.out.println("═══════════════════════════════");
        System.out.println("  STREAM API & OPTIONAL DEMO");
        System.out.println("═══════════════════════════════");

        streamBasics();
        advancedCollectors();
        flatMapDemo();
        optionalDemo();

        System.out.println("\n── Key Takeaways ──");
        System.out.println("  ✓ Stream: declarative — mô tả WHAT, không phải HOW");
        System.out.println("  ✓ filter/map/sorted: intermediate (lazy) — chỉ chạy khi có terminal");
        System.out.println("  ✓ collect/findFirst/count: terminal operation — trigger execution");
        System.out.println("  ✓ groupingBy: rất hay dùng khi group data trả về cho client");
        System.out.println("  ✓ flatMap: dùng khi mỗi element map ra một Collection");
        System.out.println("  ✓ Optional.orElseThrow: chuẩn nhất cho service layer (ném 404)");
        System.out.println("  ✓ KHÔNG dùng Optional.get() trực tiếp — không an toàn");
    }
}
