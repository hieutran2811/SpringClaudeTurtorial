package com.example.springclaudeturtorial.phase2.di;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.stereotype.*;

import java.util.List;
import java.util.Map;

/**
 * TOPIC: Dependency Injection — 3 kiểu inject + xử lý conflict
 */
public class DependencyInjectionDemo {

    // ── Interfaces ─────────────────────────────────────────────────────────
    interface PaymentGateway {
        String charge(double amount);
        String name();
    }

    interface ReportGenerator {
        String generate(String data);
    }

    // ── Implementations ────────────────────────────────────────────────────
    @Component("vnpay")
    static class VnPayGateway implements PaymentGateway {
        public String charge(double amount) { return "VnPay charged " + amount; }
        public String name() { return "VnPay"; }
    }

    @Component("momo")
    @Primary  // ← bean mặc định khi có nhiều PaymentGateway, không dùng @Qualifier
    static class MoMoGateway implements PaymentGateway {
        public String charge(double amount) { return "MoMo charged " + amount; }
        public String name() { return "MoMo"; }
    }

    @Component("zalopay")
    static class ZaloPayGateway implements PaymentGateway {
        public String charge(double amount) { return "ZaloPay charged " + amount; }
        public String name() { return "ZaloPay"; }
    }

    @Component
    @Scope("prototype") // mỗi lần inject/getBean → instance mới
    static class PdfReportGenerator implements ReportGenerator {
        private final String id = java.util.UUID.randomUUID().toString().substring(0, 6);
        public String generate(String data) { return "[PDF-" + id + "] " + data; }
    }


    // ════════════════════════════════════════════════════════════════════════
    // CÁCH 1: Constructor Injection — LUÔN dùng cái này
    // ════════════════════════════════════════════════════════════════════════
    @Service
    static class OrderService {
        private final PaymentGateway paymentGateway; // @Primary → MoMo được inject
        private final List<PaymentGateway> allGateways; // inject TẤT CẢ implementation

        // @Autowired không cần ghi nếu chỉ có 1 constructor (Spring 4.3+)
        public OrderService(
                @Primary PaymentGateway paymentGateway,
                List<PaymentGateway> allGateways) {
            this.paymentGateway = paymentGateway;
            this.allGateways    = allGateways;
        }

        public String processOrder(double amount) {
            return "OrderService → " + paymentGateway.charge(amount);
        }

        public void showAllGateways() {
            System.out.println("  All payment gateways:");
            allGateways.forEach(g -> System.out.println("    - " + g.name()));
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // CÁCH 2: @Qualifier — chỉ định chính xác bean nào inject
    // ════════════════════════════════════════════════════════════════════════
    @Service
    static class CheckoutService {
        private final PaymentGateway vnpay;   // inject đúng VnPay
        private final PaymentGateway momo;    // inject đúng MoMo

        public CheckoutService(
                @Qualifier("vnpay") PaymentGateway vnpay,
                @Qualifier("momo")  PaymentGateway momo) {
            this.vnpay = vnpay;
            this.momo  = momo;
        }

        public void checkout(double amount) {
            System.out.println("  VnPay: " + vnpay.charge(amount));
            System.out.println("  MoMo:  " + momo.charge(amount));
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // CÁCH 3: Inject vào Map<String, Interface> — key là bean name
    // ════════════════════════════════════════════════════════════════════════
    @Service
    static class PaymentRouter {
        private final Map<String, PaymentGateway> gateways; // key = bean name

        public PaymentRouter(Map<String, PaymentGateway> gateways) {
            this.gateways = gateways;
        }

        public String route(String gatewayName, double amount) {
            PaymentGateway gw = gateways.get(gatewayName);
            if (gw == null) return "Gateway not found: " + gatewayName;
            return gw.charge(amount);
        }

        public void showAvailable() {
            System.out.println("  Available gateways: " + gateways.keySet());
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // GOTCHA: Prototype bean inject vào Singleton
    // ════════════════════════════════════════════════════════════════════════

    // BAD: prototype inject trực tiếp vào singleton → chỉ tạo 1 lần!
    @Service
    static class ReportServiceBAD {
        @Autowired
        private ReportGenerator reportGenerator; // inject 1 lần khi singleton tạo

        public String generate(String data) {
            // reportGenerator.id luôn giống nhau dù gọi nhiều lần
            return reportGenerator.generate(data);
        }
    }

    // GOOD: dùng ObjectProvider → tạo instance mới mỗi lần
    @Service
    static class ReportServiceGOOD {
        private final ObjectProvider<ReportGenerator> reportProvider;

        public ReportServiceGOOD(ObjectProvider<ReportGenerator> reportProvider) {
            this.reportProvider = reportProvider;
        }

        public String generate(String data) {
            ReportGenerator rg = reportProvider.getObject(); // tạo instance mới mỗi lần
            return rg.generate(data);
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // @Value injection
    // ════════════════════════════════════════════════════════════════════════
    @Service
    static class AppConfigService {
        @Value("${app.name:Spring Tutorial}")      // key:defaultValue
        private String appName;

        @Value("${app.max-connections:10}")
        private int maxConnections;

        @Value("${app.features.enabled:true}")
        private boolean featuresEnabled;

        @Value("#{2 * 3 + 1}")                    // SpEL expression
        private int spelResult;

        @Value("#{T(java.lang.Runtime).getRuntime().availableProcessors()}")
        private int cpuCount;

        public void printConfig() {
            System.out.println("  appName         = " + appName);
            System.out.println("  maxConnections  = " + maxConnections);
            System.out.println("  featuresEnabled = " + featuresEnabled);
            System.out.println("  SpEL (2*3+1)    = " + spelResult);
            System.out.println("  CPU cores       = " + cpuCount);
        }
    }


    // ════════════════════════════════════════════════════════════════════════
    // Configuration
    // ════════════════════════════════════════════════════════════════════════
    @Configuration
    @ComponentScan(basePackageClasses = DependencyInjectionDemo.class)
    static class AppConfig {}


    public static void main(String[] args) {
        System.out.println("═══════════════════════════════");
        System.out.println("  DEPENDENCY INJECTION DEMO");
        System.out.println("═══════════════════════════════");

        var ctx = new AnnotationConfigApplicationContext(AppConfig.class);

        // ── 1. Constructor injection + @Primary ───────────────────────────────
        System.out.println("\n[1] Constructor Injection + @Primary");
        OrderService orderSvc = ctx.getBean(OrderService.class);
        System.out.println("  " + orderSvc.processOrder(500_000)); // MoMo (@Primary)
        orderSvc.showAllGateways();

        // ── 2. @Qualifier ─────────────────────────────────────────────────────
        System.out.println("\n[2] @Qualifier — chỉ định đúng bean");
        CheckoutService checkoutSvc = ctx.getBean(CheckoutService.class);
        checkoutSvc.checkout(1_000_000);

        // ── 3. Map injection ──────────────────────────────────────────────────
        System.out.println("\n[3] Map<String, Interface> injection");
        PaymentRouter router = ctx.getBean(PaymentRouter.class);
        router.showAvailable();
        System.out.println("  Route to vnpay:   " + router.route("vnpay",   200_000));
        System.out.println("  Route to zalopay: " + router.route("zalopay", 300_000));
        System.out.println("  Route to unknown: " + router.route("paypal",  400_000));

        // ── 4. Prototype gotcha ───────────────────────────────────────────────
        System.out.println("\n[4] Prototype Singleton Gotcha");
        ReportServiceBAD  badSvc  = ctx.getBean(ReportServiceBAD.class);
        ReportServiceGOOD goodSvc = ctx.getBean(ReportServiceGOOD.class);

        System.out.println("  BAD  (same instance each call):");
        System.out.println("    " + badSvc.generate("report-1"));
        System.out.println("    " + badSvc.generate("report-2")); // id GIỐNG nhau
        System.out.println("    " + badSvc.generate("report-3")); // id GIỐNG nhau

        System.out.println("  GOOD (new instance each call):");
        System.out.println("    " + goodSvc.generate("report-1"));
        System.out.println("    " + goodSvc.generate("report-2")); // id KHÁC nhau
        System.out.println("    " + goodSvc.generate("report-3")); // id KHÁC nhau

        // ── 5. @Value ─────────────────────────────────────────────────────────
        System.out.println("\n[5] @Value injection");
        ctx.getBean(AppConfigService.class).printConfig();

        ctx.close();

        System.out.println("\n── Key Takeaways ──");
        System.out.println("  ✓ Constructor injection: immutable, dễ test, lộ rõ dependency");
        System.out.println("  ✓ @Primary: bean mặc định khi inject không chỉ định @Qualifier");
        System.out.println("  ✓ @Qualifier: inject đúng bean khi cần cụ thể");
        System.out.println("  ✓ List<Interface>: inject tất cả implementation — Strategy pattern");
        System.out.println("  ✓ Map<String, Interface>: key = bean name — dynamic routing");
        System.out.println("  ✓ ObjectProvider: inject prototype vào singleton đúng cách");
        System.out.println("  ✓ @Value: inject config từ properties/yml, hỗ trợ SpEL");
    }
}
