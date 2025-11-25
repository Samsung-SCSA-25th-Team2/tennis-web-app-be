package com.example.scsa.batch;

import com.example.scsa.domain.vo.MatchStatus;
import com.example.scsa.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
/**
 * 매치 상태 자동 업데이트 배치 Job/Step 설정
 *
 * - Job 이름: matchStatusUpdateJob
 * - Step 이름: closeExpiredMatchesStep
 * - 역할: 현재 시간 기준으로 이미 시작한(RECRUITING 상태) 매치를 COMPLETED로 일괄 변경
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MatchStatusBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MatchRepository matchRepository;

    /**
     * 배치 Job 정의
     */
    @Bean
    public Job matchStatusUpdateJob() {
        return new JobBuilder("matchStatusUpdateJob", jobRepository)
                .start(closeExpiredMatchesStep())
                .build();
    }

    /**
     * 만료된 매치를 일괄 COMPLETED로 변경하는 Step
     */
    @Bean
    public Step closeExpiredMatchesStep() {
        return new StepBuilder("closeExpiredMatchesStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDateTime now = LocalDateTime.now();

                    int updatedCount = matchRepository.completeExpiredMatches(
                            now,
                            MatchStatus.RECRUITING,
                            MatchStatus.COMPLETED
                    );

                    log.info("[Batch] Expired match auto-complete executed. now={}, updatedCount={}", now, updatedCount);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}