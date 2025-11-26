package com.example.scsa.repository;

import com.example.scsa.domain.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat 엔티티 Repository
 * 채팅 메시지 관련 데이터베이스 접근을 담당
 *
 * N+1 문제 해결:
 * - fetch join을 활용하여 sender, chatRoom을 한 번에 조회
 * - 메시지 목록 조회 시 유저 정보도 함께 로드
 */
@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    /**
     * 읽지 않은 메세지 개수 카운팅
     * @param chatRoomId 채팅방 ID
     * @param userId 유저 ID
     * @return 개수 반환
     */
    @Query("""
        SELECT COUNT(c)
        FROM Chat c
        WHERE c.chatRoom.id = :chatRoomId
          AND c.sender.id <> :userId
          AND c.isRead = false
        """)
    long countUnreadMessages(Long chatRoomId, Long userId);

    /**
     * 특정 채팅방에서, 현재 사용자(currentUserId)가 보낸 메시지가 아니면서
     * 아직 isRead = false 인 메시지들을 모두 읽음 처리한다.
     *
     * @return 변경된 row 수 (updatedCount)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           UPDATE Chat c
              SET c.isRead = true,
                  c.readAt = :readAt
            WHERE c.chatRoom.id = :chatRoomId
              AND c.sender.id <> :currentUserId
              AND c.isRead = false
           """)
    int markMessagesAsRead(@Param("chatRoomId") Long chatRoomId,
                           @Param("currentUserId") Long currentUserId,
                           @Param("readAt") LocalDateTime readAt);

    /**
     * 특정 채팅방에서, 현재 사용자(currentUserId)가 보낸 메시지
     * @param roomId 채팅방 ID
     * @param cursor 커서
     * @param limit  메세지 가져올 개수
     * @return 채팅 리스트
     */
    @Query(value = """
        SELECT *
          FROM chat
         WHERE chat_room_id = :roomId
           AND (:cursor IS NULL OR created_at < :cursor)
         ORDER BY created_at DESC
         LIMIT :limit
        """, nativeQuery = true)
    List<Chat> findChatsByRoomWithCursor(
            @Param("roomId") Long roomId,
            @Param("cursor") LocalDateTime cursor,
            @Param("limit") int limit);
}