package com.example.scsa.repository;

import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.entity.User;
import com.example.scsa.domain.vo.GameType;
import com.example.scsa.domain.vo.MatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
     * 모집중인 매치 목록 조회 - 전체 (N+1 방지: host fetch join)
     * 매치 목록 화면에서 사용 (host 정보 필요)
     *
     * N+1 해결:
     * - fetch join으로 host를 한 번에 조회
     * - 매치 목록 조회 시 host 정보(닉네임, 프로필 등)도 함께 로드
     *
     * @param matchStatus 매치 상태 (RECRUITING, COMPLETED 등)
     * @return 해당 상태의 매치 목록 (host 포함)
     */
    @Query("SELECT DISTINCT m FROM Match m " +
           "LEFT JOIN FETCH m.host " +
           "WHERE m.matchStatus = :matchStatus " +
           "ORDER BY m.matchStartDateTime ASC")
    List<Match> findByMatchStatus(@Param("matchStatus") MatchStatus matchStatus);

    /**
     * 모집중인 매치 목록 조회 - 페이지네이션 (Page)
     *
     * 사용 시나리오:
     * - 페이지 번호를 표시해야 할 때 (1, 2, 3... 페이지)
     * - 전체 매치 개수가 필요할 때
     *
     * 페이지네이션:
     * - countQuery를 별도로 작성하여 전체 개수 조회
     * - fetch join은 실제 데이터 조회에만 사용
     *
     * @param matchStatus 매치 상태
     * @param pageable 페이지 정보 (페이지 번호, 크기, 정렬)
     * @return 페이지 정보와 매치 목록 (host 포함)
     */
    @Query(value = "SELECT DISTINCT m FROM Match m " +
                   "LEFT JOIN FETCH m.host " +
                   "WHERE m.matchStatus = :matchStatus",
           countQuery = "SELECT COUNT(DISTINCT m) FROM Match m " +
                        "WHERE m.matchStatus = :matchStatus")
    Page<Match> findByMatchStatusPage(
            @Param("matchStatus") MatchStatus matchStatus,
            Pageable pageable
    );

    /**
     * 모집중인 매치 목록 조회 - 페이지네이션 (Slice)
     *
     * 사용 시나리오:
     * - 무한 스크롤 구현 시
     * - 전체 개수가 필요 없을 때 (성능 최적화)
     *
     * 페이지네이션:
     * - count query를 실행하지 않아 성능 향상
     * - hasNext()로 다음 페이지 존재 여부만 확인
     *
     * @param matchStatus 매치 상태
     * @param pageable 페이지 정보
     * @return 슬라이스 정보와 매치 목록 (host 포함)
     */
    @Query("SELECT DISTINCT m FROM Match m " +
           "LEFT JOIN FETCH m.host " +
           "WHERE m.matchStatus = :matchStatus")
    Slice<Match> findByMatchStatusSlice(
            @Param("matchStatus") MatchStatus matchStatus,
            Pageable pageable
    );

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
     * 특정 유저가 호스트인 매치 목록 조회 - 전체 (N+1 방지: host fetch join)
     *
     * 사용 시나리오:
     * - 내가 만든 매치 목록 조회
     *
     * @param host 호스트 유저
     * @return 호스트가 만든 매치 목록
     */
    @Query("SELECT DISTINCT m FROM Match m " +
           "LEFT JOIN FETCH m.host " +
           "WHERE m.host = :host " +
           "ORDER BY m.matchStartDateTime DESC")
    List<Match> findByHost(@Param("host") User host);

    /**
     * 특정 유저가 호스트인 매치 목록 조회 - 페이지네이션 (Page)
     *
     * @param host 호스트 유저
     * @param pageable 페이지 정보
     * @return 페이지 정보와 매치 목록 (host 포함)
     */
    @Query(value = "SELECT DISTINCT m FROM Match m " +
                   "LEFT JOIN FETCH m.host " +
                   "WHERE m.host = :host",
           countQuery = "SELECT COUNT(DISTINCT m) FROM Match m " +
                        "WHERE m.host = :host")
    Page<Match> findByHostPage(
            @Param("host") User host,
            Pageable pageable
    );

    /**
     * 특정 유저가 호스트인 매치 목록 조회 - 페이지네이션 (Slice)
     *
     * @param host 호스트 유저
     * @param pageable 페이지 정보
     * @return 슬라이스 정보와 매치 목록 (host 포함)
     */
    @Query("SELECT DISTINCT m FROM Match m " +
           "LEFT JOIN FETCH m.host " +
           "WHERE m.host = :host")
    Slice<Match> findByHostSlice(
            @Param("host") User host,
            Pageable pageable
    );

    /**
     * 특정 유저가 참여한 모든 매치 조회 - 전체
     * host이거나 matchGuests에 포함된 매치 모두 반환
     *
     * 사용 시나리오:
     * - 내가 참여한 매치 목록 조회 (호스트 + 게스트 모두 포함)
     *
     * N+1 해결:
     * - fetch join으로 host, matchGuests를 한 번에 조회
     * - DISTINCT로 중복 제거 (fetch join 시 Cartesian Product 방지)
     *
     * 주의:
     * - OneToMany(matchGuests) fetch join + pagination은 메모리 페이징 발생
     * - 많은 데이터 조회 시 페이지네이션 버전 사용 권장
     *
     * @param user 조회할 유저
     * @return 유저가 참여한 매치 목록 (host, matchGuests 포함)
     */
    @Query("SELECT DISTINCT m FROM Match m " +
           "LEFT JOIN FETCH m.host " +
           "LEFT JOIN FETCH m.matchGuests mg " +
           "WHERE m.host = :user OR mg.user = :user " +
           "ORDER BY m.matchStartDateTime DESC")
    List<Match> findByUserParticipating(@Param("user") User user);

    /**
     * 특정 유저가 참여한 모든 매치 조회 - 페이지네이션 (Page)
     *
     * 주의사항:
     * - OneToMany fetch join(matchGuests)을 제거하여 페이징 성능 최적화
     * - matchGuests가 필요한 경우 별도로 조회하거나 Batch Size 설정 필요
     * - 또는 findByIdWithGuests()로 상세 조회
     *
     * @param user 조회할 유저
     * @param pageable 페이지 정보
     * @return 페이지 정보와 매치 목록 (host 포함, matchGuests 미포함)
     */
    @Query(value = "SELECT DISTINCT m FROM Match m " +
                   "LEFT JOIN FETCH m.host " +
                   "LEFT JOIN m.matchGuests mg " +
                   "WHERE m.host = :user OR mg.user = :user",
           countQuery = "SELECT COUNT(DISTINCT m) FROM Match m " +
                        "LEFT JOIN m.matchGuests mg " +
                        "WHERE m.host = :user OR mg.user = :user")
    Page<Match> findByUserParticipatingPage(
            @Param("user") User user,
            Pageable pageable
    );

    /**
     * 특정 유저가 참여한 모든 매치 조회 - 페이지네이션 (Slice)
     *
     * @param user 조회할 유저
     * @param pageable 페이지 정보
     * @return 슬라이스 정보와 매치 목록 (host 포함, matchGuests 미포함)
     */
    @Query("SELECT DISTINCT m FROM Match m " +
           "LEFT JOIN FETCH m.host " +
           "LEFT JOIN m.matchGuests mg " +
           "WHERE m.host = :user OR mg.user = :user")
    Slice<Match> findByUserParticipatingSlice(
            @Param("user") User user,
            Pageable pageable
    );

    /**
     * 특정 기간 내 매치 목록 조회 - 전체 (N+1 방지: host fetch join)
     *
     * 사용 시나리오:
     * - 오늘의 매치, 이번 주 매치 등
     *
     * @param startDateTime 시작 시간
     * @param endDateTime 종료 시간
     * @param matchStatus 매치 상태
     * @return 기간 내 매치 목록
     */
    @Query("SELECT DISTINCT m FROM Match m " +
           "LEFT JOIN FETCH m.host " +
           "WHERE m.matchStartDateTime >= :startDateTime " +
           "AND m.matchStartDateTime < :endDateTime " +
           "AND m.matchStatus = :matchStatus " +
           "ORDER BY m.matchStartDateTime ASC")
    List<Match> findByDateRange(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("matchStatus") MatchStatus matchStatus
    );

    /**
     * 특정 기간 내 매치 목록 조회 - 페이지네이션 (Page)
     *
     * @param startDateTime 시작 시간
     * @param endDateTime 종료 시간
     * @param matchStatus 매치 상태
     * @param pageable 페이지 정보
     * @return 페이지 정보와 매치 목록 (host 포함)
     */
    @Query(value = "SELECT DISTINCT m FROM Match m " +
                   "LEFT JOIN FETCH m.host " +
                   "WHERE m.matchStartDateTime >= :startDateTime " +
                   "AND m.matchStartDateTime < :endDateTime " +
                   "AND m.matchStatus = :matchStatus",
           countQuery = "SELECT COUNT(DISTINCT m) FROM Match m " +
                        "WHERE m.matchStartDateTime >= :startDateTime " +
                        "AND m.matchStartDateTime < :endDateTime " +
                        "AND m.matchStatus = :matchStatus")
    Page<Match> findByDateRangePage(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("matchStatus") MatchStatus matchStatus,
            Pageable pageable
    );

    /**
     * 특정 기간 내 매치 목록 조회 - 페이지네이션 (Slice)
     *
     * @param startDateTime 시작 시간
     * @param endDateTime 종료 시간
     * @param matchStatus 매치 상태
     * @param pageable 페이지 정보
     * @return 슬라이스 정보와 매치 목록 (host 포함)
     */
    @Query("SELECT DISTINCT m FROM Match m " +
           "LEFT JOIN FETCH m.host " +
           "WHERE m.matchStartDateTime >= :startDateTime " +
           "AND m.matchStartDateTime < :endDateTime " +
           "AND m.matchStatus = :matchStatus")
    Slice<Match> findByDateRangeSlice(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("matchStatus") MatchStatus matchStatus,
            Pageable pageable
    );

    /**
     * 특정 유저가 참여한 매치 개수
     *
     * @param user 조회할 유저
     * @return 참여 중인 매치 개수
     */
    @Query("SELECT COUNT(DISTINCT m) FROM Match m " +
           "LEFT JOIN m.matchGuests mg " +
           "WHERE m.host = :user OR mg.user = :user")
    long countByUserParticipating(@Param("user") User user);

    /**
     * 특정 코트의 매치 목록 조회 (기간 범위)
     *
     * 사용 시나리오:
     * - 특정 코트의 예약 현황 조회
     * - 코트별 매치 일정 확인
     *
     * @param courtId 코트 ID
     * @param start 시작 시간
     * @param end 종료 시간
     * @return 해당 코트의 매치 목록
     */
    @Query("SELECT m FROM Match m " +
           "LEFT JOIN FETCH m.host " +
           "WHERE m.court.id = :courtId " +
           "AND m.matchStartDateTime BETWEEN :start AND :end " +
           "ORDER BY m.matchStartDateTime ASC")
    List<Match> findByCourtIdAndDateRange(
        @Param("courtId") Long courtId,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );

    /**
     * 특정 코트의 모든 매치 조회
     *
     * 사용 시나리오:
     * - 코트별 전체 매치 내역
     *
     * @param courtId 코트 ID
     * @return 해당 코트의 모든 매치
     */
    @Query("SELECT m FROM Match m " +
           "LEFT JOIN FETCH m.host " +
           "WHERE m.court.id = :courtId " +
           "ORDER BY m.matchStartDateTime DESC")
    List<Match> findByCourtId(@Param("courtId") Long courtId);

    /**
     * 기본 필터:
     *  - 시작 시간이 주어진 구간 안
     *  - gameType (nullable이면 전체)
     *  - status IN (...)
     *  - cursor 이전 match까지(optional)
     * 정렬은 Service에서 직접 한다.
     */
    @Query("""
            select m
            from Match m
            join fetch m.host h
            join fetch m.court c
            where m.matchStartDateTime >= :startDateTime
              and m.matchEndDateTime <= :endDateTime
              and (:gameType is null or m.gameType = :gameType)
              and m.matchStatus in :statuses
              and (:cursorId is null or m.id < :cursorId)
            """)
    List<Match> findForList(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            @Param("gameType") GameType gameType,
            @Param("statuses") Set<MatchStatus> statuses,
            @Param("cursorId") Long cursorId
    );

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
}