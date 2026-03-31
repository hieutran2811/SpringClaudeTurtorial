package com.example.springclaudeturtorial.phase1.java17;

import java.util.Objects;

/**
 * TOPIC: Records (Java 16+)
 *
 * Record tự động sinh: constructor, getter, equals, hashCode, toString
 * Immutable by default — phù hợp làm DTO, Value Object
 */
public class RecordsDemo {

    // ── 1. Record cơ bản ──────────────────────────────────────────────────────
    // Thay thế hoàn toàn class POJO dưới đây:
    //
    //   public class UserDto {
    //       private final Long id;
    //       private final String name;
    //       private final String email;
    //       public UserDto(Long id, String name, String email) { ... }
    //       public Long getId() { ... }
    //       // + equals, hashCode, toString — ~40 dòng
    //   }
    //
    record UserDto(Long id, String name, String email) {}


    // ── 2. Record với validation trong compact constructor ────────────────────
    record Money(double amount, String currency) {
        // Compact constructor — thêm validation không cần gán lại field
        Money {
            if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
            if (currency == null || currency.isBlank()) throw new IllegalArgumentException("Currency required");
            currency = currency.toUpperCase(); // normalize trước khi gán
        }
    }


    // ── 3. Record với custom method ───────────────────────────────────────────
    record Point(double x, double y) {
        // Thêm business method bình thường
        double distanceTo(Point other) {
            return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2));
        }

        // Static factory method — naming convention rõ ràng hơn constructor
        static Point origin() {
            return new Point(0, 0);
        }
    }


    // ── 4. Record implement interface ─────────────────────────────────────────
    interface Printable {
        void print();
    }

    record Invoice(String id, double total) implements Printable {
        @Override
        public void print() {
            System.out.printf("Invoice #%s — Total: %.2f%n", id, total);
        }
    }


    // ── 5. Generic Record ─────────────────────────────────────────────────────
    record ApiResponse<T>(int statusCode, String message, T data) {
        boolean isSuccess() {
            return statusCode >= 200 && statusCode < 300;
        }
    }


    public static void main(String[] args) {
        System.out.println("═══════════════════════════════");
        System.out.println("  RECORDS DEMO");
        System.out.println("═══════════════════════════════");

        // ── 1. Dùng UserDto record ────────────────────────────────────────────
        UserDto user = new UserDto(1L, "Hieu", "hieu@example.com");

        System.out.println("\n[1] Basic Record");
        System.out.println("  user        = " + user);           // toString tự sinh
        System.out.println("  user.name() = " + user.name());    // getter: tên field, không có get prefix
        System.out.println("  user.email()= " + user.email());

        // equals dựa trên giá trị, không phải reference
        UserDto user2 = new UserDto(1L, "Hieu", "hieu@example.com");
        System.out.println("  user == user2  (reference) : " + (user == user2));    // false
        System.out.println("  user.equals(user2) (value) : " + user.equals(user2)); // true


        // ── 2. Record với validation ──────────────────────────────────────────
        System.out.println("\n[2] Record với Compact Constructor Validation");
        Money price = new Money(100.0, "usd");
        System.out.println("  price = " + price); // currency tự uppercase thành USD

        try {
            new Money(-50, "VND");
        } catch (IllegalArgumentException e) {
            System.out.println("  Caught: " + e.getMessage());
        }


        // ── 3. Record với custom method ───────────────────────────────────────
        System.out.println("\n[3] Record với Custom Method");
        Point a = new Point(0, 0);
        Point b = new Point(3, 4);
        System.out.printf("  Distance A→B = %.1f%n", a.distanceTo(b)); // 5.0

        Point origin = Point.origin();
        System.out.println("  Origin = " + origin);


        // ── 4. Record implement interface ─────────────────────────────────────
        System.out.println("\n[4] Record implement Interface");
        Invoice invoice = new Invoice("INV-001", 1_500_000.00);
        invoice.print();


        // ── 5. Generic Record ─────────────────────────────────────────────────
        System.out.println("\n[5] Generic Record (ApiResponse)");
        ApiResponse<UserDto> response = new ApiResponse<>(200, "OK", user);
        System.out.println("  status  = " + response.statusCode());
        System.out.println("  success = " + response.isSuccess());
        System.out.println("  data    = " + response.data());

        ApiResponse<Void> errorResponse = new ApiResponse<>(404, "Not Found", null);
        System.out.println("  error   = " + errorResponse);


        // ── Key takeaways ─────────────────────────────────────────────────────
        System.out.println("\n── Key Takeaways ──");
        System.out.println("  ✓ Record là immutable — không có setter");
        System.out.println("  ✓ Getter không có tiền tố 'get': user.name() thay vì user.getName()");
        System.out.println("  ✓ equals() dựa trên VALUE, không phải reference");
        System.out.println("  ✓ Compact constructor để validate / normalize data");
        System.out.println("  ✓ Dùng cho: DTO, API response, Value Object trong DDD");
    }
}
