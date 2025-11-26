package com.example.scsa.repository;

import com.example.scsa.domain.entity.MatchGuest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * MatchGuest 엔티티 Repository
 * 매치 참가자 관련 데이터베이스 접근을 담당
 *
 * N+1 문제 해결:
 * - fetch join을 활용하여 match, user를 한 번에 조회
 * - 참가자 목록 조회 시 유저/매치 정보도 함께 로드
 */
@Repository
public interface MatchGuestRepository extends JpaRepository<MatchGuest, Long> {

     /**
     * 특정 유저가 참가한 MatchGuest 목록 삭제
     * @param userId
     */
    void deleteByUser_Id(Long userId);
}
