package com.example.scsa.repository;

import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.entity.MatchGuest;
import com.example.scsa.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     * 특정 매치의 참가자 목록 조회 (N+1 방지: user fetch join)
     *
     * 사용 시나리오:
     * - 매치 상세 화면에서 참가자 목록 표시
     * - 매치 참가 가능 여부 확인 (정원 체크)
     *
     * N+1 해결:
     * - fetch join으로 user를 한 번에 조회
     * - 참가자 표시 시 필요한 유저 정보(닉네임, 프로필 등) 함께 로드
     *
     * 정렬:
     * - 호스트가 먼저 표시되도록 정렬 (match.host와 user 비교)
     *
     * @param match 매치
     * @return 해당 매치의 참가자 목록 (user 포함)
     */
    @Query("SELECT mg FROM MatchGuest mg " +
           "LEFT JOIN FETCH mg.user u " +
           "WHERE mg.match = :match " +
           "ORDER BY CASE WHEN u.id = mg.match.host.id THEN 0 ELSE 1 END, mg.id ASC")
    List<MatchGuest> findByMatch(@Param("match") Match match);

    /**
     * 특정 유저가 참가한 MatchGuest 목록 조회 (N+1 방지: match fetch join)
     *
     * 사용 시나리오:
     * - 내가 참가한 매치 목록 조회
     * - 유저별 매치 참가 이력 확인
     *
     * N+1 해결:
     * - fetch join으로 match를 한 번에 조회
     * - 매치 정보(시간, 장소 등) 함께 로드
     *
     * @param user 유저
     * @return 유저가 참가한 MatchGuest 목록 (match 포함)
     */
    @Query("SELECT mg FROM MatchGuest mg " +
           "LEFT JOIN FETCH mg.match " +
           "WHERE mg.user = :user " +
           "ORDER BY mg.match.matchStartDateTime DESC")
    List<MatchGuest> findByUser(@Param("user") User user);

    /**
     * 특정 매치의 호스트 조회 (N+1 방지: user fetch join)
     *
     * 사용 시나리오:
     * - 매치 생성자 정보 확인
     * - 호스트 권한 체크
     *
     * 참고:
     * - isHost 필드 대신 match.host와 user를 비교하여 호스트 확인
     *
     * @param match 매치
     * @return 매치의 호스트 (user 포함)
     */
    @Query("SELECT mg FROM MatchGuest mg " +
           "LEFT JOIN FETCH mg.user u " +
           "WHERE mg.match = :match AND u.id = mg.match.host.id")
    Optional<MatchGuest> findHostByMatch(@Param("match") Match match);

    /**
     * 특정 매치의 일반 참가자 목록 조회 (호스트 제외)
     *
     * 사용 시나리오:
     * - 호스트를 제외한 참가자 목록 표시
     *
     * 참고:
     * - isHost 필드 대신 match.host와 user를 비교하여 호스트 제외
     *
     * @param match 매치
     * @return 일반 참가자 목록 (user 포함)
     */
    @Query("SELECT mg FROM MatchGuest mg " +
           "LEFT JOIN FETCH mg.user u " +
           "WHERE mg.match = :match AND u.id != mg.match.host.id " +
           "ORDER BY mg.id ASC")
    List<MatchGuest> findGuestsByMatch(@Param("match") Match match);

    /**
     * 매치-유저 조합으로 MatchGuest 조회 (N+1 방지: user, match fetch join)
     *
     * 사용 시나리오:
     * - 특정 유저가 특정 매치에 참가했는지 확인
     * - 중복 참가 방지
     *
     * @param match 매치
     * @param user 유저
     * @return 해당 조합의 MatchGuest
     */
    @Query("SELECT mg FROM MatchGuest mg " +
           "LEFT JOIN FETCH mg.user " +
           "LEFT JOIN FETCH mg.match " +
           "WHERE mg.match = :match AND mg.user = :user")
    Optional<MatchGuest> findByMatchAndUser(
            @Param("match") Match match,
            @Param("user") User user
    );

    /**
     * 특정 매치에 특정 유저가 참가했는지 확인
     *
     * 사용 시나리오:
     * - 중복 참가 방지
     * - 참가 여부 빠른 확인
     *
     * @param match 매치
     * @param user 유저
     * @return 참가 여부 (true: 참가중, false: 미참가)
     */
    @Query("SELECT COUNT(mg) > 0 FROM MatchGuest mg " +
           "WHERE mg.match = :match AND mg.user = :user")
    boolean existsByMatchAndUser(
            @Param("match") Match match,
            @Param("user") User user
    );

    /**
     * 특정 매치의 참가자 수 조회
     *
     * 사용 시나리오:
     * - 정원 확인
     * - 매치 참가 가능 여부 체크
     *
     * @param match 매치
     * @return 참가자 수 (호스트 포함)
     */
    @Query("SELECT COUNT(mg) FROM MatchGuest mg WHERE mg.match = :match")
    long countByMatch(@Param("match") Match match);

    /**
     * 특정 유저의 매치 참가 횟수
     *
     * @param user 유저
     * @return 참가 횟수
     */
    @Query("SELECT COUNT(mg) FROM MatchGuest mg WHERE mg.user = :user")
    long countByUser(@Param("user") User user);

    /**
     * 특정 유저가 참가한 MatchGuest 목록 조회 - 페이징 (N+1 방지: match, court, host fetch join)
     *
     * 사용 시나리오:
     * - 내가 참가한 매치 목록 조회 (페이징)
     * - 무한 스크롤 구현
     *
     * N+1 해결:
     * - fetch join으로 match, court, host를 한 번에 조회
     * - 매치 목록 화면에 필요한 모든 정보 포함
     *
     * @param userId 유저 ID
     * @param pageable 페이지 정보
     * @return 유저가 참가한 MatchGuest 페이지 (match, court, host 포함)
     */
    @Query(value = "SELECT mg FROM MatchGuest mg " +
                   "LEFT JOIN FETCH mg.match m " +
                   "LEFT JOIN FETCH m.court " +
                   "LEFT JOIN FETCH m.host " +
                   "WHERE mg.user.id = :userId " +
                   "ORDER BY m.matchStartDateTime DESC",
           countQuery = "SELECT COUNT(mg) FROM MatchGuest mg " +
                        "WHERE mg.user.id = :userId")
    Page<MatchGuest> findByUserIdWithMatch(
        @Param("userId") Long userId,
        Pageable pageable
    );

}
