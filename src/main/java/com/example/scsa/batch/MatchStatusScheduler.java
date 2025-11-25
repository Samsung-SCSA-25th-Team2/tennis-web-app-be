package com.example.scsa.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 매치 상태 업데이트 배치 Job을 주기적으로 실행하는 스케줄러
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class MatchStatusScheduler {

    private final JobLauncher jobLauncher;
    private final Job matchStatusUpdateJob;


    @Scheduled(cron = "0 */5 * * * *")   // TODO: 필요에 따라 주기 조정
    public void runMatchStatusUpdateJob() {
        try {
            // JobParameters는 매 실행마다 달라야 새로운 JobInstance로 인식됨
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            log.info("[Scheduler] Starting matchStatusUpdateJob with params={}", params);
            jobLauncher.run(matchStatusUpdateJob, params);
        } catch (Exception e) {
            log.error("[Scheduler] Failed to run matchStatusUpdateJob", e);
        }
    }
}