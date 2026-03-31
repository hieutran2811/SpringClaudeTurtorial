package com.example.springclaudeturtorial.phase1.java17;

/**
 * TOPIC: Sealed Classes (Java 17) + Pattern Matching
 *
 * Sealed class: kiểm soát chính xác những class nào được phép kế thừa.
 * Pattern matching: switch / instanceof gọn hơn, type-safe hơn.
 */
public class SealedAndPatternDemo {

    // ── 1. Sealed Interface — Domain model rõ ràng ───────────────────────────
    // Chỉ 3 loại này được phép implement Shape, không ai khác
    sealed interface Shape permits Circle, Rectangle, Triangle {}

    record Circle(double radius) implements Shape {}
    record Rectangle(double width, double height) implements Shape {}
    record Triangle(double base, double height) implements Shape {}


    // ── 2. Tính diện tích — Pattern Matching + Switch Expression ─────────────
    static double calculateArea(Shape shape) {
        return switch (shape) {
            case Circle c    -> Math.PI * c.radius() * c.radius();
            case Rectangle r -> r.width() * r.height();
            case Triangle t  -> 0.5 * t.base() * t.height();
            // Không cần 'default' vì compiler biết đã cover hết (sealed!)
        };
    }

    static String describe(Shape shape) {
        return switch (shape) {
            case Circle c    when c.radius() > 10 -> "Large circle r=" + c.radius();
            case Circle c                         -> "Small circle r=" + c.radius();
            case Rectangle r when r.width() == r.height() -> "Square " + r.width() + "x" + r.height();
            case Rectangle r -> "Rectangle " + r.width() + "x" + r.height();
            case Triangle t  -> "Triangle base=" + t.base();
        };
    }


    // ── 3. Sealed class cho Payment domain ───────────────────────────────────
    sealed interface PaymentResult permits PaymentResult.Success, PaymentResult.Failure, PaymentResult.Pending {
        record Success(String transactionId, double amount) implements PaymentResult {}
        record Failure(String reason, int errorCode)       implements PaymentResult {}
        record Pending(String referenceId)                 implements PaymentResult {}
    }

    static String handlePayment(PaymentResult result) {
        return switch (result) {
            case PaymentResult.Success s  -> "✓ Paid " + s.amount() + " — txn: " + s.transactionId();
            case PaymentResult.Failure f  -> "✗ Failed: " + f.reason() + " (code " + f.errorCode() + ")";
            case PaymentResult.Pending p  -> "⏳ Pending — ref: " + p.referenceId();
        };
    }


    // ── 4. Pattern Matching với instanceof ───────────────────────────────────
    static void processObject(Object obj) {
        // Cũ (Java 15 trở về trước)
        if (obj instanceof String) {
            String s = (String) obj;    // phải cast thủ công
            System.out.println("  Old way: String length = " + s.length());
        }

        // Mới (Java 16+) — cast và bind trong 1 bước
        if (obj instanceof String s) {
            System.out.println("  New way: String length = " + s.length());
        } else if (obj instanceof Integer i && i > 0) {
            System.out.println("  Positive integer: " + i);
        } else if (obj instanceof Double d) {
            System.out.printf("  Double: %.3f%n", d);
        }
    }


    // ── 5. Sealed class cho Error handling (thực tế) ─────────────────────────
    sealed interface Result<T> permits Result.Ok, Result.Err {
        record Ok<T>(T value)       implements Result<T> {}
        record Err<T>(String error) implements Result<T> {}

        // Factory methods
        static <T> Result<T> ok(T value)     { return new Ok<>(value); }
        static <T> Result<T> err(String msg) { return new Err<>(msg); }
    }

    static Result<Integer> divide(int a, int b) {
        if (b == 0) return Result.err("Division by zero");
        return Result.ok(a / b);
    }


    public static void main(String[] args) {
        System.out.println("═══════════════════════════════");
        System.out.println("  SEALED CLASSES & PATTERN MATCHING");
        System.out.println("═══════════════════════════════");

        // ── 1. Shape area ─────────────────────────────────────────────────────
        System.out.println("\n[1] Shape Area Calculation");
        Shape[] shapes = {
            new Circle(5),
            new Rectangle(4, 6),
            new Triangle(3, 8)
        };
        for (Shape s : shapes) {
            System.out.printf("  %-30s → area = %.2f%n", describe(s), calculateArea(s));
        }


        // ── 2. Payment result handling ────────────────────────────────────────
        System.out.println("\n[2] Payment Result Handling");
        PaymentResult[] results = {
            new PaymentResult.Success("TXN-123", 500_000),
            new PaymentResult.Failure("Insufficient funds", 4001),
            new PaymentResult.Pending("REF-456")
        };
        for (PaymentResult r : results) {
            System.out.println("  " + handlePayment(r));
        }


        // ── 3. instanceof pattern matching ────────────────────────────────────
        System.out.println("\n[3] instanceof Pattern Matching");
        Object[] objects = { "Hello Spring", 42, -5, 3.14159 };
        for (Object o : objects) {
            processObject(o);
        }


        // ── 4. Result type ────────────────────────────────────────────────────
        System.out.println("\n[4] Result<T> — Functional Error Handling");
        int[][] pairs = { {10, 2}, {7, 0}, {9, 3} };
        for (int[] p : pairs) {
            Result<Integer> result = divide(p[0], p[1]);
            String output = switch (result) {
                case Result.Ok<Integer> ok   -> p[0] + " / " + p[1] + " = " + ok.value();
                case Result.Err<Integer> err -> p[0] + " / " + p[1] + " → Error: " + err.error();
            };
            System.out.println("  " + output);
        }


        // ── Key takeaways ─────────────────────────────────────────────────────
        System.out.println("\n── Key Takeaways ──");
        System.out.println("  ✓ Sealed: compiler kiểm tra đã xử lý hết cases — không cần default");
        System.out.println("  ✓ Pattern matching switch: gọn hơn, type-safe hơn if-else chain");
        System.out.println("  ✓ 'when' guard clause: filter điều kiện ngay trong case");
        System.out.println("  ✓ Result<T> pattern: encode success/failure vào type thay vì throw exception");
        System.out.println("  ✓ SA dùng Sealed class để model domain states rõ ràng (DDD)");
    }
}
