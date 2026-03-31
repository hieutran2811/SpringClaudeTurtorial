package com.example.springclaudeturtorial.phase1.solid;

import java.util.List;

/**
 * TOPIC: SOLID Principles
 *
 * 5 nguyên tắc thiết kế hướng đối tượng — nền tảng của Spring IoC/DI.
 * Mỗi phần có ví dụ BAD → GOOD để thấy rõ sự khác biệt.
 */
public class SolidDemo {

    // ════════════════════════════════════════════════════════════════════════
    // S — Single Responsibility Principle
    // Mỗi class chỉ có 1 lý do để thay đổi
    // ════════════════════════════════════════════════════════════════════════

    // BAD: OrderService làm quá nhiều việc
    static class OrderServiceBAD {
        public void placeOrder(String product, int qty) {
            // 1. Business logic
            System.out.println("  [BAD] Processing order: " + product + " x" + qty);
            // 2. Persistence — nên tách ra Repository
            System.out.println("  [BAD] INSERT INTO orders...");
            // 3. Email — nên tách ra NotificationService
            System.out.println("  [BAD] Sending email confirmation...");
            // 4. Logging — nên dùng AOP
            System.out.println("  [BAD] Writing to audit log...");
        }
        // Nếu thay đổi template email → phải sửa OrderService
        // Nếu thay đổi DB schema    → phải sửa OrderService
    }

    // GOOD: Mỗi class 1 trách nhiệm
    interface OrderRepository      { void save(String product, int qty); }
    interface NotificationService  { void sendConfirmation(String product); }
    interface AuditLogger          { void log(String action); }

    static class OrderRepositoryImpl implements OrderRepository {
        public void save(String product, int qty) {
            System.out.println("  [GOOD] Saved order: " + product + " x" + qty);
        }
    }

    static class EmailNotificationService implements NotificationService {
        public void sendConfirmation(String product) {
            System.out.println("  [GOOD] Email sent for: " + product);
        }
    }

    static class DatabaseAuditLogger implements AuditLogger {
        public void log(String action) {
            System.out.println("  [GOOD] Audit: " + action);
        }
    }

    static class OrderServiceGOOD {
        private final OrderRepository repo;
        private final NotificationService notifier;
        private final AuditLogger auditor;

        OrderServiceGOOD(OrderRepository r, NotificationService n, AuditLogger a) {
            this.repo = r; this.notifier = n; this.auditor = a;
        }

        public void placeOrder(String product, int qty) {
            repo.save(product, qty);
            notifier.sendConfirmation(product);
            auditor.log("ORDER_PLACED: " + product);
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // O — Open/Closed Principle
    // Mở để extend, đóng để sửa — thêm tính năng mà không sửa code cũ
    // ════════════════════════════════════════════════════════════════════════

    // BAD: thêm discount type → phải sửa DiscountService
    static class DiscountServiceBAD {
        public double applyDiscount(String type, double price) {
            if ("STUDENT".equals(type))    return price * 0.8;
            if ("EMPLOYEE".equals(type))   return price * 0.7;
            // Thêm "MEMBER" → phải sửa class này ← vi phạm OCP
            return price;
        }
    }

    // GOOD: thêm loại discount mới → tạo class mới, không sửa code cũ
    interface DiscountStrategy {
        double apply(double price);
        String type();
    }

    record StudentDiscount()  implements DiscountStrategy {
        public double apply(double price) { return price * 0.8; }
        public String type() { return "STUDENT"; }
    }
    record EmployeeDiscount() implements DiscountStrategy {
        public double apply(double price) { return price * 0.7; }
        public String type() { return "EMPLOYEE"; }
    }
    record MemberDiscount()   implements DiscountStrategy {  // Thêm mới — không sửa gì cũ
        public double apply(double price) { return price * 0.9; }
        public String type() { return "MEMBER"; }
    }

    static class DiscountServiceGOOD {
        private final List<DiscountStrategy> strategies;

        DiscountServiceGOOD(List<DiscountStrategy> strategies) {
            this.strategies = strategies;
        }

        public double applyDiscount(String type, double price) {
            return strategies.stream()
                .filter(s -> s.type().equals(type))
                .findFirst()
                .map(s -> s.apply(price))
                .orElse(price);
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // L — Liskov Substitution Principle
    // Subtype phải thay thế được supertype mà không làm hỏng behavior
    // ════════════════════════════════════════════════════════════════════════

    // BAD: Square vi phạm LSP khi kế thừa Rectangle
    static class RectangleBAD {
        protected double width, height;
        public void setWidth(double w)  { this.width  = w; }
        public void setHeight(double h) { this.height = h; }
        public double area() { return width * height; }
    }

    static class SquareBAD extends RectangleBAD {
        @Override
        public void setWidth(double w)  { this.width = this.height = w; }  // phá vỡ contract của Rectangle!
        @Override
        public void setHeight(double h) { this.width = this.height = h; }
    }

    // Test sẽ fail với SquareBAD dù test đúng với Rectangle
    static void testArea(RectangleBAD r) {
        r.setWidth(5);
        r.setHeight(3);
        double expected = 15;
        double actual   = r.area();
        System.out.println("  Expected: " + expected + ", Got: " + actual
            + (actual == expected ? " ✓" : " ✗ LSP violated!"));
    }

    // GOOD: tách biệt hoàn toàn
    interface Shape { double area(); }
    record RectangleGOOD(double width, double height) implements Shape {
        public double area() { return width * height; }
    }
    record SquareGOOD(double side) implements Shape {
        public double area() { return side * side; }
    }


    // ════════════════════════════════════════════════════════════════════════
    // I — Interface Segregation Principle
    // Interface nhỏ, chuyên biệt — client không bị ép implement những gì không dùng
    // ════════════════════════════════════════════════════════════════════════

    // BAD: Interface quá béo
    interface WorkerBAD {
        void work();
        void eat();
        void sleep();
        void attendMeeting();
    }

    // Robot không ăn, không ngủ → phải implement method rỗng — vi phạm ISP
    static class RobotBAD implements WorkerBAD {
        public void work()          { System.out.println("  [ISP] Robot working"); }
        public void eat()           { /* Robot không ăn — để trống, sai! */ }
        public void sleep()         { /* Robot không ngủ — để trống, sai! */ }
        public void attendMeeting() { /* ... */ }
    }

    // GOOD: Tách thành interface nhỏ
    interface Workable   { void work(); }
    interface Eatable    { void eat(); }
    interface Sleepable  { void sleep(); }
    interface Meetable   { void attendMeeting(); }

    static class HumanWorker implements Workable, Eatable, Sleepable, Meetable {
        public void work()          { System.out.println("  [ISP] Human working"); }
        public void eat()           { System.out.println("  [ISP] Human eating"); }
        public void sleep()         { System.out.println("  [ISP] Human sleeping"); }
        public void attendMeeting() { System.out.println("  [ISP] Human in meeting"); }
    }

    static class Robot implements Workable, Meetable {
        public void work()          { System.out.println("  [ISP] Robot working"); }
        public void attendMeeting() { System.out.println("  [ISP] Robot in meeting"); }
        // Không cần eat, sleep — hợp lý!
    }


    // ════════════════════════════════════════════════════════════════════════
    // D — Dependency Inversion Principle
    // High-level module không phụ thuộc low-level module — cả 2 phụ thuộc abstraction
    // ĐÂY CHÍNH LÀ CƠ CHẾ IoC/DI CỦA SPRING
    // ════════════════════════════════════════════════════════════════════════

    // BAD: PaymentService tự tạo dependency (tight coupling)
    static class VnPayGateway {
        public boolean charge(double amount) {
            System.out.println("  [BAD] VnPay charging: " + amount);
            return true;
        }
    }

    static class PaymentServiceBAD {
        private VnPayGateway gateway = new VnPayGateway(); // hard-coded!
        // Muốn đổi sang MoMo → phải sửa PaymentService
        // Không test được mà không hit VnPay thật

        public boolean processPayment(double amount) {
            return gateway.charge(amount);
        }
    }

    // GOOD: Phụ thuộc vào abstraction (interface)
    interface PaymentGateway {
        boolean charge(double amount);
    }

    static class VnPayGatewayGOOD implements PaymentGateway {
        public boolean charge(double amount) {
            System.out.println("  [GOOD] VnPay charging: " + amount);
            return true;
        }
    }

    static class MoMoGateway implements PaymentGateway {
        public boolean charge(double amount) {
            System.out.println("  [GOOD] MoMo charging: " + amount);
            return true;
        }
    }

    static class MockPaymentGateway implements PaymentGateway {
        public boolean charge(double amount) {
            System.out.println("  [TEST] Mock charging: " + amount);
            return true; // luôn thành công trong test
        }
    }

    static class PaymentServiceGOOD {
        private final PaymentGateway gateway; // phụ thuộc vào interface

        // Constructor injection — Spring inject cái này tự động
        PaymentServiceGOOD(PaymentGateway gateway) {
            this.gateway = gateway;
        }

        public boolean processPayment(double amount) {
            return gateway.charge(amount);
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // MAIN
    // ════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════");
        System.out.println("  SOLID PRINCIPLES DEMO");
        System.out.println("═══════════════════════════════");

        // S — SRP
        System.out.println("\n[S] Single Responsibility");
        new OrderServiceBAD().placeOrder("Laptop", 1);
        OrderServiceGOOD good = new OrderServiceGOOD(
            new OrderRepositoryImpl(), new EmailNotificationService(), new DatabaseAuditLogger()
        );
        good.placeOrder("Laptop", 1);

        // O — OCP
        System.out.println("\n[O] Open/Closed");
        DiscountServiceGOOD discountSvc = new DiscountServiceGOOD(
            List.of(new StudentDiscount(), new EmployeeDiscount(), new MemberDiscount())
        );
        System.out.println("  STUDENT  10M → " + discountSvc.applyDiscount("STUDENT",  10_000_000));
        System.out.println("  EMPLOYEE 10M → " + discountSvc.applyDiscount("EMPLOYEE", 10_000_000));
        System.out.println("  MEMBER   10M → " + discountSvc.applyDiscount("MEMBER",   10_000_000));
        System.out.println("  UNKNOWN  10M → " + discountSvc.applyDiscount("UNKNOWN",  10_000_000));

        // L — LSP
        System.out.println("\n[L] Liskov Substitution");
        System.out.print("  Rectangle (expected 15): ");
        testArea(new RectangleBAD());
        System.out.print("  Square BAD (expected 15): ");
        testArea(new SquareBAD()); // sẽ ra 9, không phải 15

        // I — ISP
        System.out.println("\n[I] Interface Segregation");
        new HumanWorker().work();
        new Robot().work();

        // D — DIP (= Spring IoC/DI)
        System.out.println("\n[D] Dependency Inversion (= Spring IoC)");
        // Production: inject VnPay
        PaymentServiceGOOD prodService = new PaymentServiceGOOD(new VnPayGatewayGOOD());
        prodService.processPayment(500_000);

        // Đổi sang MoMo: không sửa PaymentService, chỉ đổi dependency
        PaymentServiceGOOD momoService = new PaymentServiceGOOD(new MoMoGateway());
        momoService.processPayment(500_000);

        // Test: inject mock — không cần gateway thật
        PaymentServiceGOOD testService = new PaymentServiceGOOD(new MockPaymentGateway());
        testService.processPayment(500_000);

        System.out.println("\n── Key Takeaways ──");
        System.out.println("  S: Mỗi class 1 lý do thay đổi → dễ test, dễ maintain");
        System.out.println("  O: Thêm tính năng bằng class mới, không sửa class cũ");
        System.out.println("  L: Subclass không phá vỡ contract của parent");
        System.out.println("  I: Interface nhỏ → implement chỉ cái mình cần");
        System.out.println("  D: Phụ thuộc abstraction → Spring inject implementation phù hợp");
        System.out.println("  ✓ Spring hiện thực hóa SOLID qua: @Component, @Service, @Autowired");
    }
}
