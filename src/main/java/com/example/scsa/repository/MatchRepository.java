package com.example.scsa.repository;

import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.vo.MatchStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Match 엔티티 Repository
 * 테니스 매치 관련 데이터베이스 접근을 담당
 *
 * N+1 문제 해결:
 * - fetch join을 적극 활용하여 연관 엔티티를 한 번에 조회
 * - 상황에 따라 필요한 데이터만 조회 (목록 vs 상세)
 *
 * 페이지네이션:
 * - Page vs Slice:
 *   - Page: 전체 개수 조회 (count query 실행) → 페이지 번호 표시 필요할 때
 *   - Slice: 전체 개수 미조회 (다음 페이지 여부만 확인) → 무한 스크롤에 적합
 * - fetch join + pagination 사용 시 countQuery 필수 분리
 */
@Repository
public interface MatchRepository extends JpaRepository<Match, Long>, MatchRepositoryCustom {

    /**
     * 매치 상세 조회 (N+1 방지: host, matchGuests 모두 fetch join)
     * 매치 상세 화면에서 사용 (참가자 목록 필요)
     *
     * N+1 해결:
     * - fetch join으로 host, matchGuests를 한 번에 조회
     * - matchGuests의 user 정보까지 함께 조회 (2단계 fetch join)
     *
     * @param id 매치 ID
     * @return 매치 상세 정보 (host, matchGuests, 각 guest의 user 포함)
     */
    @Query("SELECT DISTINCT m FROM Match m " +
           "LEFT JOIN FETCH m.host " +
           "LEFT JOIN FETCH m.matchGuests mg " +
           "LEFT JOIN FETCH mg.user " +
           "WHERE m.id = :id")
    Optional<Match> findByIdWithGuests(@Param("id") Long id);

    /**
     * 해당 유저가 호스트이고, 상태가 RECRUITING 인 매치가 하나라도 있는지
     * @param hostId
     * @param matchStatus
     * @return
     */
    boolean existsByHost_IdAndMatchStatus(Long hostId, MatchStatus matchStatus);

    /**
     * 해당 유저가 호스트이고, 상태가 COMPLETED 인 매치가 하나라도 있는지
     * @param hostId
     * @param matchStatus
     */
    void deleteAllByHost_IdAndMatchStatus(Long hostId, MatchStatus matchStatus);

    /**
     * 현재시간을 비교하여 현재 상태가 recruiting인 매치를 completed로 변경
     * @param now 현재 시간
     * @param recruiting 현재 상태
     * @param completed 변경할 상태
     * @return 변경된 행 개수
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE Match m
           SET m.matchStatus = :completed
         WHERE m.matchStartDateTime < :now
           AND m.matchStatus = :recruiting
        """)
    int completeExpiredMatches(@Param("now") LocalDateTime now,
                               @Param("recruiting") MatchStatus recruiting,
                               @Param("completed") MatchStatus completed);

    /**
     * hostId로 검색한 매치 목록 matchId 로 내림차순 정렬로 반환
     * @param hostId hostId
     * @param pageable 페이징 정보
     * @return 매치 목록
     */
    Slice<Match> findByHostIdOrderByIdDesc(Long hostId, Pageable pageable);

    /**
     * hostId로 검색한 매치 목록들을 cursor(마지막 matchId)보다 작은 것들만 내림차순 정렬로 반환
     * @param hostId hostId
     * @param cursor 마지막 matchId
     * @param pageable 페이징 정보
     * @return 매치 목록
     */
    Slice<Match> findByHostIdAndIdLessThanOrderByIdDesc(Long hostId, Long cursor, Pageable pageable);
}