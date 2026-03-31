package com.example.springclaudeturtorial.phase2.container;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * TOPIC: IoC Container & Bean Lifecycle
 *
 * Minh họa toàn bộ vòng đời của một Spring Bean từ khi tạo đến khi destroy.
 */
public class BeanLifecycleDemo {

    // ── 1. Bean thể hiện đầy đủ lifecycle callbacks ──────────────────────────
    @Component
    static class FullLifecycleBean implements BeanNameAware, ApplicationContextAware,
                                               InitializingBean, DisposableBean {

        private String beanName;
        private ApplicationContext applicationContext;
        private String data;

        // BƯỚC 1: Constructor — Spring tạo object
        public FullLifecycleBean() {
            System.out.println("  [1] Constructor called");
        }

        // BƯỚC 2: BeanNameAware — Spring thông báo tên bean
        @Override
        public void setBeanName(String name) {
            this.beanName = name;
            System.out.println("  [2] setBeanName: " + name);
        }

        // BƯỚC 3: ApplicationContextAware — inject ApplicationContext
        @Override
        public void setApplicationContext(ApplicationContext ctx) throws BeansException {
            this.applicationContext = ctx;
            System.out.println("  [3] setApplicationContext: " + ctx.getClass().getSimpleName());
        }

        // BƯỚC 4a: InitializingBean.afterPropertiesSet() — sau khi inject xong
        @Override
        public void afterPropertiesSet() throws Exception {
            System.out.println("  [4a] afterPropertiesSet (InitializingBean)");
        }

        // BƯỚC 4b: @PostConstruct — SAU inject, TRƯỚC bean ready (khuyến nghị hơn 4a)
        @PostConstruct
        public void postConstruct() {
            this.data = "initialized-data";
            System.out.println("  [4b] @PostConstruct: data = " + data);
        }

        // BƯỚC 5: Bean sẵn sàng sử dụng
        public String getData() {
            return data;
        }

        // BƯỚC 6a: @PreDestroy — trước khi destroy
        @PreDestroy
        public void preDestroy() {
            System.out.println("  [6a] @PreDestroy: cleanup started");
        }

        // BƯỚC 6b: DisposableBean.destroy()
        @Override
        public void destroy() throws Exception {
            System.out.println("  [6b] destroy() (DisposableBean): releasing resources");
        }
    }


    // ── 2. Bean Scopes ────────────────────────────────────────────────────────
    @Component
    static class SingletonBean {
        private final String id = java.util.UUID.randomUUID().toString().substring(0, 8);
        public String getId() { return id; }
    }

    @Component
    @Scope("prototype")
    static class PrototypeBean {
        private final String id = java.util.UUID.randomUUID().toString().substring(0, 8);
        public String getId() { return id; }
    }


    // ── 3. Bean với @Bean lifecycle hooks trong @Configuration ───────────────
    @Configuration
    static class AppConfig {

        // initMethod / destroyMethod — dùng khi class không thể có @PostConstruct
        // (ví dụ: class từ thư viện bên ngoài)
        @Bean(initMethod = "start", destroyMethod = "stop")
        public ExternalService externalService() {
            return new ExternalService("https://api.example.com");
        }

        @Bean
        public FullLifecycleBean fullLifecycleBean() {
            return new FullLifecycleBean();
        }

        @Bean
        public SingletonBean singletonBean() {
            return new SingletonBean();
        }

        @Bean
        @Scope("prototype")
        public PrototypeBean prototypeBean() {
            return new PrototypeBean();
        }
    }

    // Giả lập external library class (không thể sửa thêm annotation)
    static class ExternalService {
        private final String url;
        ExternalService(String url) { this.url = url; }

        public void start() {
            System.out.println("  [ExternalService] Connected to: " + url);
        }

        public void stop() {
            System.out.println("  [ExternalService] Disconnected from: " + url);
        }

        public String call() { return "response from " + url; }
    }


    public static void main(String[] args) {
        System.out.println("═══════════════════════════════");
        System.out.println("  BEAN LIFECYCLE DEMO");
        System.out.println("═══════════════════════════════");

        // Tạo ApplicationContext — đây là IoC Container
        System.out.println("\n▶ Creating ApplicationContext...");
        AnnotationConfigApplicationContext ctx =
            new AnnotationConfigApplicationContext(AppConfig.class);

        // ── Bean Lifecycle ────────────────────────────────────────────────────
        System.out.println("\n[Lifecycle] FullLifecycleBean đã qua các bước 1→4b");
        FullLifecycleBean lifecycleBean = ctx.getBean(FullLifecycleBean.class);
        System.out.println("  Bean ready, data = " + lifecycleBean.getData());

        // ── Singleton scope ────────────────────────────────────────────────────
        System.out.println("\n[Scope] Singleton — luôn cùng instance");
        SingletonBean s1 = ctx.getBean(SingletonBean.class);
        SingletonBean s2 = ctx.getBean(SingletonBean.class);
        System.out.println("  s1.id = " + s1.getId());
        System.out.println("  s2.id = " + s2.getId());
        System.out.println("  s1 == s2 : " + (s1 == s2)); // true

        // ── Prototype scope ────────────────────────────────────────────────────
        System.out.println("\n[Scope] Prototype — instance MỚI mỗi lần getBean");
        PrototypeBean p1 = ctx.getBean(PrototypeBean.class);
        PrototypeBean p2 = ctx.getBean(PrototypeBean.class);
        System.out.println("  p1.id = " + p1.getId());
        System.out.println("  p2.id = " + p2.getId());
        System.out.println("  p1 == p2 : " + (p1 == p2)); // false

        // ── ExternalService ────────────────────────────────────────────────────
        System.out.println("\n[ExternalService] Gọi service:");
        ExternalService extSvc = ctx.getBean(ExternalService.class);
        System.out.println("  " + extSvc.call());

        // ── ApplicationContext info ────────────────────────────────────────────
        System.out.println("\n[Context] Các bean đang được quản lý:");
        String[] beanNames = ctx.getBeanDefinitionNames();
        for (String name : beanNames) {
            if (!name.startsWith("org.springframework")) { // lọc bớt Spring internal beans
                System.out.println("  - " + name);
            }
        }

        // ── Đóng context — trigger @PreDestroy / destroy() ────────────────────
        System.out.println("\n▶ Closing ApplicationContext (trigger destroy callbacks)...");
        ctx.close();

        System.out.println("\n── Key Takeaways ──");
        System.out.println("  ✓ Lifecycle: Constructor → Aware → @PostConstruct → Ready → @PreDestroy → destroy");
        System.out.println("  ✓ @PostConstruct: dùng để load cache, connect pool sau khi inject xong");
        System.out.println("  ✓ @PreDestroy:    dùng để đóng connection, flush buffer khi app tắt");
        System.out.println("  ✓ Singleton: 1 instance/context — thread-safe state phải cẩn thận");
        System.out.println("  ✓ Prototype: instance mới mỗi lần — Spring KHÔNG gọi @PreDestroy!");
        System.out.println("  ✓ initMethod/destroyMethod: cho class từ thư viện không sửa được");
    }
}
