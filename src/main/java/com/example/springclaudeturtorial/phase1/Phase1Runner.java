package com.example.springclaudeturtorial.phase1;

import com.example.springclaudeturtorial.phase1.java17.*;
import com.example.springclaudeturtorial.phase1.patterns.DesignPatternsDemo;
import com.example.springclaudeturtorial.phase1.solid.SolidDemo;

/**
 * Runner để chạy toàn bộ Phase 1 demo.
 * Chạy class này như standalone Java app (không cần Spring context).
 */
public class Phase1Runner {

    public static void main(String[] args) throws Exception {
        System.out.println("╔═══════════════════════════════════════╗");
        System.out.println("║   PHASE 1 — Java & OOP Foundation     ║");
        System.out.println("╚═══════════════════════════════════════╝");

        separator("1/6 — Records");
        RecordsDemo.main(args);

        separator("2/6 — Sealed Classes & Pattern Matching");
        SealedAndPatternDemo.main(args);

        separator("3/6 — Stream API & Optional");
        StreamAndOptionalDemo.main(args);

        separator("4/6 — CompletableFuture");
        CompletableFutureDemo.main(args);

        separator("5/6 — SOLID Principles");
        SolidDemo.main(args);

        separator("6/6 — Design Patterns");
        DesignPatternsDemo.main(args);

        System.out.println("\n╔═══════════════════════════════════════╗");
        System.out.println("║   Phase 1 Complete! Sẵn sàng Phase 2  ║");
        System.out.println("╚═══════════════════════════════════════╝");
    }

    static void separator(String title) {
        System.out.println("\n\n▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓");
        System.out.println("  Topic " + title);
        System.out.println("▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓");
    }
}
