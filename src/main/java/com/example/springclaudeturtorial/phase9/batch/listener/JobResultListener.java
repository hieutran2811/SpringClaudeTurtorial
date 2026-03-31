package com.example.springclaudeturtorial.phase9.batch.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * ============================================================
 * PHASE 9 — Spring Batch: JobExecutionListener
 * ============================================================
 *
 * JobExecutionListener: hook vào lifecycle của Job.
 *   beforeJob() → trước khi Job bắt đầu
 *   afterJob()  → sau khi Job kết thúc (dù SUCCESS hay FAILED)
 *
 * Dùng để: logging, gửi notification, cleanup resources.
 *
 * JobExecution chứa:
 *   - JobInstance (jobName + jobParameters)
 *   - status: STARTING, STARTED, STOPPING, STOPPED, FAILED, COMPLETED, ABANDONED
 *   - startTime, endTime, exitStatus
 *   - StepExecutions — chi tiết từng Step
 * ============================================================
 */
public class JobResultListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobResultListener.class);

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("╔══════════════════════════════════════════╗");
        log.info("║   [BATCH] Job Starting                   ║");
        log.info("╚══════════════════════════════════════════╝");
        log.info("Job: {} | Instance: {} | Params: {}",
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobInstance().getInstanceId(),
                jobExecution.getJobParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        var status = jobExecution.getStatus();

        log.info("╔══════════════════════════════════════════╗");
        log.info("║   [BATCH] Job Finished: {}          ║", status);
        log.info("╚══════════════════════════════════════════╝");

        // Tổng hợp thống kê từ tất cả Steps
        jobExecution.getStepExecutions().forEach(step -> {
            log.info("  Step '{}': read={} processed={} written={} skipped={} | status={}",
                    step.getStepName(),
                    step.getReadCount(),
                    step.getProcessSkipCount() == 0 ? step.getReadCount() : step.getReadCount() - step.getProcessSkipCount(),
                    step.getWriteCount(),
                    step.getProcessSkipCount() + step.getReadSkipCount() + step.getWriteSkipCount(),
                    step.getStatus());
        });

        if (status.isUnsuccessful()) {
            log.error("Job FAILED! Failures: {}", jobExecution.getAllFailureExceptions());
            // TODO: gửi alert (Slack, email, PagerDuty)
        }

        long durationMs = jobExecution.getEndTime() != null
                ? java.time.Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis()
                : -1;
        log.info("Duration: {}ms", durationMs);
    }
}
