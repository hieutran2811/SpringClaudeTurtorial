package com.example.springclaudeturtorial.phase9.batch.config;

import com.example.springclaudeturtorial.phase3.product.Product;
import com.example.springclaudeturtorial.phase9.batch.dto.ProductExportDto;
import com.example.springclaudeturtorial.phase9.batch.listener.JobResultListener;
import com.example.springclaudeturtorial.phase9.batch.processor.ProductExportProcessor;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * ============================================================
 * PHASE 9 — Spring Batch: Job Configuration
 * ============================================================
 *
 * Spring Batch 5 (Spring Boot 3+) thay đổi API:
 *   Cũ: JobBuilderFactory, StepBuilderFactory (deprecated)
 *   Mới: new JobBuilder(name, jobRepository), new StepBuilder(name, jobRepository)
 *
 * ── Kiến trúc Job ─────────────────────────────────────────
 *
 *   Job
 *   ├── Step 1: exportStep (Chunk-oriented)
 *   │     ├── ItemReader:     JpaPagingItemReader<Product>   (read từ DB)
 *   │     ├── ItemProcessor:  ProductExportProcessor          (transform)
 *   │     └── ItemWriter:     FlatFileItemWriter<ProductExportDto> (ghi CSV)
 *   └── Step 2: summaryStep (Tasklet)
 *         └── log thống kê
 *
 * ── Chunk-oriented Processing ────────────────────────────
 *
 *   Chunk size = 10 nghĩa là:
 *   1. Đọc 10 items (Reader)
 *   2. Process từng item (Processor)
 *   3. Ghi 10 items cùng lúc (Writer)  ← 1 transaction
 *   4. Lặp lại đến hết data
 *
 *   Lợi ích: không load toàn bộ data vào RAM,
 *            rollback chỉ ảnh hưởng 1 chunk nếu lỗi.
 *
 * ── @StepScope & JobParameters ───────────────────────────
 *
 *   @StepScope: Bean được tạo khi Step bắt đầu (lazy)
 *   → có thể inject JobParameters qua @Value("#{jobParameters['key']}")
 *   → mỗi lần chạy job có thể truyền tham số khác nhau
 * ============================================================
 */
@Configuration
public class ProductExportJobConfig {

    private final JobRepository            jobRepository;
    private final PlatformTransactionManager txManager;
    private final EntityManagerFactory     emf;

    public ProductExportJobConfig(JobRepository jobRepository,
                                  PlatformTransactionManager txManager,
                                  EntityManagerFactory emf) {
        this.jobRepository = jobRepository;
        this.txManager     = txManager;
        this.emf           = emf;
    }

    // ── JOB ──────────────────────────────────────────────────────────────
    @Bean
    public Job productExportJob() {
        return new JobBuilder("productExportJob", jobRepository)
                .listener(new JobResultListener())
                .start(exportStep(null, null))    // Step 1
                .next(summaryStep())              // Step 2
                .build();
    }

    // ── STEP 1: Chunk-oriented — Read → Process → Write ──────────────────
    @Bean
    public Step exportStep(
            // @Value inject từ JobParameters tại runtime
            @Value("#{jobParameters['outputPath'] ?: 'output/products.csv'}") String outputPath,
            @Value("#{jobParameters['categoryFilter']}")                       String categoryFilter) {

        return new StepBuilder("exportStep", jobRepository)
                .<Product, ProductExportDto>chunk(10, txManager)  // chunk size = 10
                .reader(productReader())
                .processor(new ProductExportProcessor(categoryFilter))
                .writer(csvWriter(outputPath))
                // Skip: bỏ qua NullPointerException, tối đa 5 items
                .faultTolerant()
                    .skip(NullPointerException.class)
                    .skipLimit(5)
                    // Retry: thử lại khi RuntimeException, tối đa 3 lần
                    .retry(RuntimeException.class)
                    .retryLimit(3)
                .build();
    }

    // ── STEP 2: Tasklet — đơn vị công việc không dùng chunk ──────────────
    //
    // Tasklet: dùng cho các tác vụ đơn giản không cần Read/Process/Write
    // Ví dụ: cleanup file cũ, gửi email thông báo, gọi stored procedure
    @Bean
    public Step summaryStep() {
        return new StepBuilder("summaryStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    var jobCtx = chunkContext.getStepContext().getStepExecution()
                            .getJobExecution();

                    long totalWritten = jobCtx.getStepExecutions().stream()
                            .mapToLong(se -> se.getWriteCount())
                            .sum();

                    org.slf4j.LoggerFactory.getLogger(getClass())
                            .info("[Summary Tasklet] Total records exported: {}", totalWritten);

                    return RepeatStatus.FINISHED;   // báo Spring Batch step đã xong
                }, txManager)
                .build();
    }

    // ── READER: JpaPagingItemReader ───────────────────────────────────────
    //
    // JpaPagingItemReader: đọc từ DB theo từng page (page-by-page)
    //   pageSize = 10 → mỗi lần query lấy 10 records
    //   Phù hợp: dataset lớn, không load toàn bộ vào RAM
    //
    // Thay thế: JdbcCursorItemReader — dùng cursor, hiệu quả hơn cho sequential read
    @Bean
    public JpaPagingItemReader<Product> productReader() {
        return new JpaPagingItemReaderBuilder<Product>()
                .name("productReader")
                .entityManagerFactory(emf)
                .queryString("SELECT p FROM Product p ORDER BY p.id")
                .pageSize(10)
                .build();
    }

    // ── WRITER: FlatFileItemWriter (CSV) ──────────────────────────────────
    //
    // FlatFileItemWriter: ghi ra text file (CSV, fixed-width, etc.)
    // BeanWrapperFieldExtractor: extract fields từ DTO theo tên
    // DelimitedLineAggregator: ghép fields bằng delimiter (comma)
    @Bean
    public FlatFileItemWriter<ProductExportDto> csvWriter(String outputPath) {
        if (outputPath == null) outputPath = "output/products.csv";

        // Tạo thư mục nếu chưa có
        new java.io.File("output").mkdirs();

        return new FlatFileItemWriterBuilder<ProductExportDto>()
                .name("csvWriter")
                .resource(new FileSystemResource(outputPath))
                .delimited()
                    .delimiter(",")
                    .names("id", "name", "category", "priceFormatted",
                           "stock", "stockStatus", "exportedAt")
                // Header line
                .headerCallback(writer -> writer.write(
                        "ID,Name,Category,Price,Stock,StockStatus,ExportedAt"))
                .build();
    }
}
