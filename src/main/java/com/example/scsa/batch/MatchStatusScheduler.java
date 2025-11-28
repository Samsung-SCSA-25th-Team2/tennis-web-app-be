package com.example.scsa.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 매치 상태 업데이트 배치 Job을 주기적으로 실행하는 스케줄러
 *
 * ShedLock을 사용하여 분산 환경에서 중복 실행 방지:
 * - 여러 서버 인스턴스가 있어도 한 서버만 스케줄러 실행
 * - lockAtMostFor: 락 최대 유지 시간 (비정상 종료 시 락 해제)
 * - lockAtLeastFor: 락 최소 유지 시간 (너무 빠른 재실행 방지)
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class MatchStatusScheduler {

    private final JobLauncher jobLauncher;
    private final Job matchStatusUpdateJob;


    @Scheduled(cron = "0 */5 * * * *")   // 5분마다 실행
    @SchedulerLock(
        name = "matchStatusUpdateJob",
        lockAtMostFor = "4m",   // 최대 4분간 락 유지 (5분보다 짧게 설정)
        lockAtLeastFor = "30s"  // 최소 30초간 락 유지 (중복 실행 방지)
    )
    public void runMatchStatusUpdateJob() {
        try {
            // JobParameters는 매 실행마다 달라야 새로운 JobInstance로 인식됨
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            log.info("[Scheduler] Starting matchStatusUpdateJob with params={}", params);
            jobLauncher.run(matchStatusUpdateJob, params);
            log.info("[Scheduler] Completed matchStatusUpdateJob");
        } catch (Exception e) {
            log.error("[Scheduler] Failed to run matchStatusUpdateJob", e);
        }
    }
}