package com.example.springclaudeturtorial.phase9.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * ============================================================
 * PHASE 9 — Spring Batch: HTTP Trigger Controller
 * ============================================================
 *
 * JobLauncher: interface để launch Job với JobParameters.
 *
 * JobParameters:
 *   - Key-value pairs truyền vào Job lúc chạy
 *   - Spring Batch dùng JobParameters để định danh JobInstance
 *   - Cùng JobName + cùng JobParameters = cùng JobInstance
 *   - → Không thể chạy lại Job đã COMPLETED với cùng parameters
 *   - Giải pháp: thêm timestamp vào parameters để mỗi lần là unique
 *
 * JobExecution vs JobInstance:
 *   JobInstance = "ý định chạy job" (logical)
 *   JobExecution = "một lần chạy cụ thể" (physical)
 *   1 JobInstance có thể có nhiều JobExecution (nếu fail và restart)
 * ============================================================
 */
@RestController
@RequestMapping("/api/v1/batch")
public class BatchController {

    private static final Logger log = LoggerFactory.getLogger(BatchController.class);

    private final JobLauncher jobLauncher;
    private final Job         productExportJob;

    public BatchController(JobLauncher jobLauncher, Job productExportJob) {
        this.jobLauncher      = jobLauncher;
        this.productExportJob = productExportJob;
    }

    // ── POST /api/v1/batch/export ─────────────────────────────────────────
    // Trigger export job với optional parameters
    @PostMapping("/export")
    public ResponseEntity<Map<String, Object>> runExportJob(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "output/products.csv") String outputPath) {

        try {
            // JobParameters: mỗi lần chạy cần unique params
            // → timestamp đảm bảo mỗi request tạo JobInstance mới
            JobParameters params = new JobParametersBuilder()
                    .addString("outputPath",      outputPath)
                    .addString("categoryFilter",  category != null ? category : "")
                    .addLong("runAt", Instant.now().toEpochMilli())   // unique key
                    .toJobParameters();

            log.info("Launching productExportJob | outputPath={} category={}", outputPath, category);

            JobExecution execution = jobLauncher.run(productExportJob, params);

            return ResponseEntity.accepted().body(Map.of(
                    "jobName",       execution.getJobInstance().getJobName(),
                    "executionId",   execution.getId(),
                    "instanceId",    execution.getJobInstance().getInstanceId(),
                    "status",        execution.getStatus().name(),
                    "outputPath",    outputPath,
                    "message",       "Job launched — check logs for progress"
            ));

        } catch (JobExecutionAlreadyRunningException e) {
            return ResponseEntity.status(409).body(Map.of(
                    "error", "Job is already running",
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Failed to launch job", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error",   "Failed to launch job",
                    "message", e.getMessage()
            ));
        }
    }

    // ── GET /api/v1/batch/export/status ───────────────────────────────────
    // Kiểm tra trạng thái job gần nhất
    @GetMapping("/export/status")
    public ResponseEntity<Map<String, Object>> getJobStatus() {
        // Trong production: query JobRepository để lấy history
        // Ở đây chỉ trả về hướng dẫn
        return ResponseEntity.ok(Map.of(
                "tip", "Check Spring Batch metadata tables: BATCH_JOB_EXECUTION",
                "h2Console", "http://localhost:8080/h2-console",
                "query", "SELECT * FROM BATCH_JOB_EXECUTION ORDER BY START_TIME DESC"
        ));
    }
}
