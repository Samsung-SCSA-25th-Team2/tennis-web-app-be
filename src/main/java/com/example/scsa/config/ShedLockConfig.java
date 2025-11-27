package com.example.scsa.config;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.boot.CommandLineRunner;
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
 * 테이블 자동 생성:
 * - 애플리케이션 시작 시 shedlock 테이블이 없으면 자동 생성
 * - CREATE TABLE IF NOT EXISTS 사용으로 안전하게 처리
 */
@Slf4j
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "10m")  // 최대 락 유지 시간 (비정상 종료 대비)
public class ShedLockConfig {

    /**
     * ShedLock 테이블 자동 생성
     * 애플리케이션 시작 시 테이블이 없으면 생성
     */
    @Bean
    public CommandLineRunner initShedLockTable(DataSource dataSource) {
        return args -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            try {
                String createTableSql = """
                    CREATE TABLE IF NOT EXISTS shedlock (
                        name VARCHAR(64) NOT NULL COMMENT '스케줄러 작업 이름',
                        lock_until TIMESTAMP(3) NOT NULL COMMENT '락 만료 시간',
                        locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '락 획득 시간',
                        locked_by VARCHAR(255) NOT NULL COMMENT '락을 획득한 서버',
                        PRIMARY KEY (name)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ShedLock 분산 스케줄러 중복 실행 방지'
                    """;

                jdbcTemplate.execute(createTableSql);
                log.info("✅ ShedLock 테이블 초기화 완료 (이미 존재하거나 새로 생성됨)");
            } catch (Exception e) {
                log.error("❌ ShedLock 테이블 생성 실패: {}", e.getMessage());
                throw new RuntimeException("ShedLock 테이블 초기화 실패", e);
            }
        };
    }

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
        return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
            .withJdbcTemplate(new JdbcTemplate(dataSource))
            .usingDbTime()  // DB 서버 시간 사용 (서버 간 시간 불일치 방지)
            .build()
        );
    }
}
