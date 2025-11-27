package com.example.scsa.repository;

import com.example.scsa.domain.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
     * 특정 매치와 두명의 유저가 존재하는 지 확인
     * @param matchId 매치 ID
     * @param user1Id 첫번째 유저 ID
     * @param user2Id 두번째 유저 ID
     * @return 존재 여부
     */
    boolean existsByMatchIdAndUser1_IdAndUser2_Id(Long matchId, Long user1Id, Long user2Id);

    /**
     * 특정 유저가 참가한 채팅방 목록을 커서 기반으로 조회.
     * @param userId 유저 ID
     * @param cursor 커서
     * @return 채팅방 리스트
     */
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

    /**
     * matchId에 따른 매치 삭제
     * @param matchId
     */
    void deleteByMatchId(Long matchId);

    /**
     * user1 이거나 user2 인 모든 채팅방 삭제
     * @param userId1
     * @param userId2
     */
    void deleteByUser1_IdOrUser2_Id(Long userId1, Long userId2);

    /**
     * matchId, userId1, userId2로 채팅방 조회
     * @param matchId
     * @param userId1
     * @param userId2
     * @return 채팅방
     */
    ChatRoom findByMatchIdAndUser1_IdAndUser2_Id(Long matchId, Long userId1, Long userId2);
}