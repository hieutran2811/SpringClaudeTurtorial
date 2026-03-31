package com.example.springclaudeturtorial.phase2;

import com.example.springclaudeturtorial.phase2.aop.AopDemo;
import com.example.springclaudeturtorial.phase2.config.ConfigurationDemo;
import com.example.springclaudeturtorial.phase2.container.BeanLifecycleDemo;
import com.example.springclaudeturtorial.phase2.di.DependencyInjectionDemo;

/**
 * Runner toàn bộ Phase 2 — Spring Core & IoC/DI
 */
public class Phase2Runner {

    public static void main(String[] args) throws Exception {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║  PHASE 2 — Spring Core & IoC/DI       ║");
        System.out.println("╚═══════════════════════════════════════╝");

        separator("1/4 — IoC Container & Bean Lifecycle");
        BeanLifecycleDemo.main(args);

        separator("2/4 — Dependency Injection");
        DependencyInjectionDemo.main(args);

        separator("3/4 — @Configuration, @Bean, @Profile");
        ConfigurationDemo.main(args);

        separator("4/4 — AOP");
        AopDemo.main(args);

        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║  Phase 2 Complete! Sẵn sàng Phase 3   ║");
        System.out.println("╚═══════════════════════════════════════╝");
    }

    static void separator(String title) {
        System.out.println("\n\n▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓");
        System.out.println("  Topic " + title);
        System.out.println("▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓");
    }
}
