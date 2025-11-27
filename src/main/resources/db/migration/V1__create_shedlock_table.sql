-- ShedLock 테이블 생성
-- 분산 환경에서 스케줄러 중복 실행 방지를 위한 락 관리 테이블
--
-- 사용법:
-- 1. 이 SQL을 운영/개발 DB에 실행하여 테이블 생성
-- 2. ShedLock이 자동으로 이 테이블을 사용하여 락 관리
--
-- 참고: Spring Boot의 ddl-auto로 자동 생성되지 않으므로 수동 실행 필요

CREATE TABLE IF NOT EXISTS shedlock (
    name VARCHAR(64) NOT NULL COMMENT '스케줄러 작업 이름 (예: matchStatusUpdateJob)',
    lock_until TIMESTAMP(3) NOT NULL COMMENT '락 만료 시간 (이 시간까지 다른 인스턴스가 실행 불가)',
    locked_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '락 획득 시간',
    locked_by VARCHAR(255) NOT NULL COMMENT '락을 획득한 서버 인스턴스 정보',
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ShedLock - 분산 스케줄러 중복 실행 방지';
