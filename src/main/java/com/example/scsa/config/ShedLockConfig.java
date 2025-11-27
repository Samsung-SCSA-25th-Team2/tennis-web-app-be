package com.example.scsa.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * ShedLock 설정
 * 분산 환경에서 여러 서버 인스턴스가 동일한 스케줄러를 실행하지 않도록 방지
 *
 * 작동 방식:
 * 1. shedlock 테이블에 lock 레코드 생성
 * 2. 스케줄러 실행 시 DB 락 획득 시도
 * 3. 락을 획득한 인스턴스만 스케줄러 실행
 * 4. 다른 인스턴스는 락 획득 실패 시 건너뜀
 *
 * DB 테이블 생성 SQL (자동 생성 안 됨, 수동 실행 필요):
 * CREATE TABLE shedlock (
 *     name VARCHAR(64) NOT NULL,
 *     lock_until TIMESTAMP(3) NOT NULL,
 *     locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
 *     locked_by VARCHAR(255) NOT NULL,
 *     PRIMARY KEY (name)
 * );
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")  // 최대 락 유지 시간 (비정상 종료 대비)
public class ShedLockConfig {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new JdbcTemplate(dataSource))
            .usingDbTime()  // DB 서버 시간 사용 (서버 간 시간 불일치 방지)
            .build()
        );
    }
}
