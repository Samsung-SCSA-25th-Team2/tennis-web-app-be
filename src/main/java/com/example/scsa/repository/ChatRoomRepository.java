package com.example.scsa.repository;

import com.example.scsa.domain.entity.ChatRoom;
import com.example.scsa.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * ChatRoom 엔티티 Repository
 * 채팅방 관련 데이터베이스 접근을 담당
 *
 * 설계 참고:
 * - 채팅방은 두 유저(user1, user2) 간의 1:1 채팅
 * - 하나의 매치에서 여러 채팅방 생성 가능 (호스트-게스트1, 호스트-게스트2, ...)
 * - match는 선택적 (매치 없이도 채팅 가능)
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * 두 유저 간의 채팅방 조회 (매치 무관, 순서 무관)
     *
     * 사용 시나리오:
     * - 채팅방이 이미 존재하는지 확인
     * - 중복 채팅방 생성 방지
     *
     * @param user1 첫 번째 유저
     * @param user2 두 번째 유저
     * @return 두 유저 간의 채팅방 (없으면 Optional.empty())
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "LEFT JOIN FETCH cr.user1 " +
           "LEFT JOIN FETCH cr.user2 " +
           "WHERE (cr.user1 = :user1 AND cr.user2 = :user2) OR " +
           "(cr.user1 = :user2 AND cr.user2 = :user1)")
    Optional<ChatRoom> findByUsers(@Param("user1") User user1, @Param("user2") User user2);

    /**
     * 특정 매치의 두 유저 간 채팅방 조회
     *
     * 사용 시나리오:
     * - 특정 매치에서 두 유저가 이미 채팅방을 만들었는지 확인
     *
     * @param matchId 매치 ID
     * @param user1 첫 번째 유저
     * @param user2 두 번째 유저
     * @return 해당 매치의 두 유저 간 채팅방
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "LEFT JOIN FETCH cr.user1 " +
           "LEFT JOIN FETCH cr.user2 " +
           "WHERE cr.matchId = :matchId AND " +
           "((cr.user1 = :user1 AND cr.user2 = :user2) OR " +
           "(cr.user1 = :user2 AND cr.user2 = :user1))")
    Optional<ChatRoom> findByMatchIdAndUsers(
        @Param("matchId") Long matchId,
        @Param("user1") User user1,
        @Param("user2") User user2
    );

    /**
     * 특정 매치의 모든 채팅방 조회
     *
     * 사용 시나리오:
     * - 매치에서 생성된 모든 채팅방 목록 확인
     *
     * @param matchId 매치 ID
     * @return 매치의 채팅방 목록
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "LEFT JOIN FETCH cr.user1 " +
           "LEFT JOIN FETCH cr.user2 " +
           "WHERE cr.matchId = :matchId " +
           "ORDER BY cr.createdAt DESC")
    List<ChatRoom> findByMatchId(@Param("matchId") Long matchId);

    /**
     * 특정 유저가 참여한 모든 채팅방 조회 (N+1 방지: user1, user2 fetch join)
     *
     * N+1 해결:
     * - fetch join으로 user1, user2를 한 번에 조회
     * - matchId는 FK로만 보관 (느슨한 결합)
     *
     * @param user 조회할 유저
     * @return 유저가 참여한 채팅방 목록
     */
    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
           "LEFT JOIN FETCH cr.user1 " +
           "LEFT JOIN FETCH cr.user2 " +
           "WHERE cr.user1 = :user OR cr.user2 = :user " +
           "ORDER BY cr.lastMessageAt DESC, cr.createdAt DESC")
    List<ChatRoom> findByUser(@Param("user") User user);

    /**
     * 특정 유저가 특정 매치에서 참여한 채팅방 목록 조회
     *
     * 사용 시나리오:
     * - 매치별 채팅방 목록 필터링
     *
     * @param user 유저
     * @param matchId 매치 ID
     * @return 해당 매치에서 유저가 참여한 채팅방 목록
     */
    @Query("SELECT cr FROM ChatRoom cr " +
           "LEFT JOIN FETCH cr.user1 " +
           "LEFT JOIN FETCH cr.user2 " +
           "WHERE cr.matchId = :matchId AND (cr.user1 = :user OR cr.user2 = :user) " +
           "ORDER BY cr.createdAt DESC")
    List<ChatRoom> findByUserIdAndMatchId(@Param("user") User user, @Param("matchId") Long matchId);

    /**
     * 채팅방 상세 조회 (N+1 방지: user1, user2, chats 모두 fetch join)
     * 채팅방과 함께 모든 메시지까지 한 번에 조회
     *
     * 사용 시나리오:
     * - 채팅방 입장 시 메시지 목록을 보여줄 때
     *
     * N+1 해결:
     * - fetch join으로 관련 엔티티를 한 번에 조회
     * - 메시지가 많을 경우 성능 이슈 가능 → 페이징 필요 시 별도 메서드 고려
     *
     * @param id 채팅방 ID
     * @return 채팅방 상세 정보 (user1, user2, chats 포함)
     */
    @Query("SELECT DISTINCT cr FROM ChatRoom cr " +
           "LEFT JOIN FETCH cr.user1 " +
           "LEFT JOIN FETCH cr.user2 " +
           "LEFT JOIN FETCH cr.chats " +
           "WHERE cr.id = :id")
    Optional<ChatRoom> findByIdWithChats(@Param("id") Long id);

    /**
     * 특정 유저가 참여한 채팅방 개수
     *
     * @param user 조회할 유저
     * @return 참여 중인 채팅방 개수
     */
    @Query("SELECT COUNT(cr) FROM ChatRoom cr " +
           "WHERE cr.user1 = :user OR cr.user2 = :user")
    long countByUser(@Param("user") User user);

    /**
     * 특정 매치의 채팅방 개수
     *
     * @param matchId 매치 ID
     * @return 매치의 채팅방 개수
     */
    @Query("SELECT COUNT(cr) FROM ChatRoom cr WHERE cr.matchId = :matchId")
    long countByMatchId(@Param("matchId") Long matchId);

    /**
     * 특정 매치와 두명의 유저가 존재하는 지 확인
     * @param matchId 매치 ID
     * @param user1Id 첫번째 유저 ID
     * @param user2Id 두번째 유저 ID
     * @return 존재 여부
     */
    boolean existsByMatchIdAndUser1_IdAndUser2_Id(Long matchId, Long user1Id, Long user2Id);

    @Query("""
        SELECT cr
        FROM ChatRoom cr
        WHERE (cr.user1.id = :userId OR cr.user2.id = :userId)
          AND (:cursor IS NULL OR cr.lastMessageAt < :cursor)
        ORDER BY cr.lastMessageAt DESC
        """)
    List<ChatRoom> findChatRoomsByUserWithCursor(
            @Param("userId") Long userId,
            @Param("cursor") LocalDateTime cursor
    );
}