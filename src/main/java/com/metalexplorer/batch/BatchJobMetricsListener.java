package com.metalexplorer.batch;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class BatchJobMetricsListener implements JobExecutionListener {

    private final MeterRegistry registry;

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        Tags jobTags = Tags.of("job", jobName);

        long durationMs = Duration.between(
                jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
        registry.timer("batch.job.duration", jobTags)
                .record(durationMs, TimeUnit.MILLISECONDS);

        int statusVal = jobExecution.getStatus() == BatchStatus.COMPLETED ? 0 : 1;
        DistributionSummary.builder("batch.job.status")
                .tags(jobTags)
                .register(registry)
                .record(statusVal);

        long totalRead = 0;
        long totalSkip = 0;
        for (var step : jobExecution.getStepExecutions()) {
            Tags stepTags = jobTags.and("step", step.getStepName());
            registry.counter("batch.job.read.count", stepTags).increment(step.getReadCount());
            registry.counter("batch.job.skip.count", stepTags).increment(step.getSkipCount());
            totalRead += step.getReadCount();
            totalSkip += step.getSkipCount();
        }

        log.info("Job '{}' finished: status={} duration={}ms read={} skip={}",
                jobName, jobExecution.getStatus(), durationMs, totalRead, totalSkip);
    }
}
