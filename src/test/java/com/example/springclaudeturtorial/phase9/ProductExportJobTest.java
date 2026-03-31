package com.example.springclaudeturtorial.phase9;

import com.example.springclaudeturtorial.phase3.product.Product;
import com.example.springclaudeturtorial.phase3.product.ProductRepository;
import org.junit.jupiter.api.*;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.*;

/**
 * ============================================================
 * PHASE 9 — Spring Batch Test
 * ============================================================
 *
 * @SpringBatchTest: auto-configure các beans dùng để test Batch:
 *   - JobLauncherTestUtils  → launch Job/Step trong test
 *   - JobRepositoryTestUtils → cleanup BATCH_* tables giữa các test
 *   - StepScopeTestExecutionListener → resolve @StepScope beans
 *
 * Test strategy:
 *   1. Test toàn bộ Job (end-to-end)
 *   2. Test từng Step riêng lẻ
 *   3. Test Processor logic độc lập (unit test)
 * ============================================================
 */
@SpringBatchTest
@SpringBootTest
@ActiveProfiles("local")
@DisplayName("ProductExportJob — Batch Tests")
class ProductExportJobTest {

    @Autowired JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired ProductRepository    productRepository;

    @BeforeEach
    void setUp() {
        // Seed test data
        productRepository.saveAll(java.util.List.of(
                new Product("Laptop",  "Electronics", 25_000_000.0, 10),
                new Product("Monitor", "Electronics", 10_000_000.0,  3),
                new Product("Chair",   "Furniture",    6_000_000.0,  0),
                new Product("Phone",   "Electronics", 15_000_000.0,  8)
        ));
    }

    @AfterEach
    void cleanUp() {
        productRepository.deleteAll();
        // Xóa output test
        new File("output/test-export.csv").delete();
    }

    // ── Test 1: Job hoàn thành với status COMPLETED ─────────────────────
    @Test
    @DisplayName("Job chạy thành công → status COMPLETED")
    void job_completesSuccessfully() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("outputPath", "output/test-export.csv")
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(execution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");
    }

    // ── Test 2: File CSV được tạo và có đúng số dòng ─────────────────────
    @Test
    @DisplayName("Job tạo file CSV với đúng số records")
    void job_createsOutputFileWithCorrectLines() throws Exception {
        String outputPath = "output/test-export.csv";

        JobParameters params = new JobParametersBuilder()
                .addString("outputPath", outputPath)
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();

        jobLauncherTestUtils.launchJob(params);

        File outputFile = new File(outputPath);
        assertThat(outputFile).exists();

        java.util.List<String> lines = Files.readAllLines(outputFile.toPath());
        // 1 header + 4 data rows
        assertThat(lines).hasSize(5);
        assertThat(lines.get(0)).contains("ID,Name,Category");  // header
        assertThat(lines.get(1)).contains("Laptop");             // data
    }

    // ── Test 3: Category filter — chỉ export Electronics ─────────────────
    @Test
    @DisplayName("Job với categoryFilter chỉ export đúng category")
    void job_withCategoryFilter_exportsOnlyMatchingProducts() throws Exception {
        String outputPath = "output/test-electronics.csv";

        JobParameters params = new JobParametersBuilder()
                .addString("outputPath",     outputPath)
                .addString("categoryFilter", "Electronics")
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();

        JobExecution execution = jobLauncherTestUtils.launchJob(params);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Kiểm tra writeCount của exportStep
        StepExecution exportStep = execution.getStepExecutions().stream()
                .filter(s -> s.getStepName().equals("exportStep"))
                .findFirst().orElseThrow();

        // 3 Electronics products được ghi (Chair/Furniture bị skip)
        assertThat(exportStep.getWriteCount()).isEqualTo(3);

        // Đọc file và verify không có Furniture
        File outputFile = new File(outputPath);
        String content  = Files.readString(outputFile.toPath());
        assertThat(content).doesNotContain("Chair");
        assertThat(content).contains("Laptop", "Monitor", "Phone");

        outputFile.delete();
    }

    // ── Test 4: Test chỉ 1 Step (không chạy toàn Job) ─────────────────
    @Test
    @DisplayName("exportStep riêng lẻ → COMPLETED với đúng readCount")
    void exportStep_runsSuccessfully() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addString("outputPath", "output/test-step.csv")
                .addLong("runAt", System.currentTimeMillis())
                .toJobParameters();

        // launchStep: chỉ chạy 1 step, không chạy toàn Job
        JobExecution execution = jobLauncherTestUtils.launchStep("exportStep", params);

        StepExecution step = execution.getStepExecutions().iterator().next();
        assertThat(step.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(step.getReadCount()).isEqualTo(4);    // 4 products seeded
        assertThat(step.getWriteCount()).isEqualTo(4);   // tất cả được ghi (không filter)

        new File("output/test-step.csv").delete();
    }

    // ── Test 5: Processor unit test ───────────────────────────────────────
    @Test
    @DisplayName("Processor transform đúng stockStatus")
    void processor_transformsStockStatus() throws Exception {
        var processor = new com.example.springclaudeturtorial.phase9.batch.processor
                .ProductExportProcessor(null);

        var outOfStock = new Product("TestOut", "Electronics", 1_000.0, 0);
        var lowStock   = new Product("TestLow", "Electronics", 1_000.0, 3);
        var inStock    = new Product("TestIn",  "Electronics", 1_000.0, 20);

        assertThat(processor.process(outOfStock).stockStatus()).isEqualTo("OUT_OF_STOCK");
        assertThat(processor.process(lowStock).stockStatus()).isEqualTo("LOW_STOCK");
        assertThat(processor.process(inStock).stockStatus()).isEqualTo("IN_STOCK");
    }

    // ── Test 6: Processor filter — trả null khi không khớp category ──────
    @Test
    @DisplayName("Processor trả null khi product không khớp category filter")
    void processor_returnsNull_whenCategoryNotMatch() throws Exception {
        var processor = new com.example.springclaudeturtorial.phase9.batch.processor
                .ProductExportProcessor("Electronics");

        var furniture = new Product("Chair", "Furniture", 5_000.0, 5);
        assertThat(processor.process(furniture)).isNull();  // bị skip
    }
}
