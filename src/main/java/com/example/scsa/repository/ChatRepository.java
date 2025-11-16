package com.example.scsa.repository;

import com.example.scsa.domain.entity.Chat;
import com.example.scsa.domain.entity.ChatRoom;
import com.example.scsa.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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
     * 특정 채팅방의 메시지 목록 조회 (N+1 방지: sender fetch join)
     *
     * 사용 시나리오:
     * - 채팅방 입장 시 메시지 목록 표시
     * - 메시지 페이징 조회
     *
     * N+1 해결:
     * - fetch join으로 sender를 한 번에 조회
     * - 메시지 표시 시 필요한 발신자 정보(닉네임, 프로필 등) 함께 로드
     *
     * 주의사항:
     * - 메시지가 많을 경우 페이징 처리 필요
     * - 실제 사용 시 Pageable 파라미터 추가 고려
     *
     * @param chatRoom 채팅방
     * @return 해당 채팅방의 메시지 목록 (sender 포함)
     */
    @Query("SELECT c FROM Chat c " +
           "LEFT JOIN FETCH c.sender " +
           "WHERE c.chatRoom = :chatRoom " +
           "ORDER BY c.createdAt ASC")
    List<Chat> findByChatRoom(@Param("chatRoom") ChatRoom chatRoom);

    /**
     * 특정 채팅방의 최근 N개 메시지 조회 (N+1 방지: sender fetch join)
     *
     * 사용 시나리오:
     * - 채팅방 목록에서 마지막 메시지 미리보기
     * - 최근 대화 내용 확인
     *
     * N+1 해결:
     * - fetch join으로 sender를 한 번에 조회
     *
     * @param chatRoom 채팅방
     * @return 최근 메시지 목록 (sender 포함)
     */
    @Query("SELECT c FROM Chat c " +
           "LEFT JOIN FETCH c.sender " +
           "WHERE c.chatRoom = :chatRoom " +
           "ORDER BY c.createdAt DESC")
    List<Chat> findByChatRoomOrderByCreatedAtDesc(@Param("chatRoom") ChatRoom chatRoom);

    /**
     * 특정 채팅방의 최신 메시지 1개 조회 (N+1 방지: sender fetch join)
     *
     * 사용 시나리오:
     * - 채팅방 목록에서 마지막 메시지 표시
     *
     * @param chatRoom 채팅방
     * @return 최신 메시지 (sender 포함)
     */
    @Query("SELECT c FROM Chat c " +
           "LEFT JOIN FETCH c.sender " +
           "WHERE c.chatRoom = :chatRoom " +
           "ORDER BY c.createdAt DESC " +
           "LIMIT 1")
    Optional<Chat> findLatestByChatRoom(@Param("chatRoom") ChatRoom chatRoom);

    /**
     * 특정 유저가 보낸 메시지 목록 조회 (N+1 방지: chatRoom fetch join)
     *
     * 사용 시나리오:
     * - 내가 보낸 메시지 목록 조회
     *
     * @param sender 발신자
     * @return 발신자가 보낸 메시지 목록 (chatRoom 포함)
     */
    @Query("SELECT c FROM Chat c " +
           "LEFT JOIN FETCH c.chatRoom " +
           "WHERE c.sender = :sender " +
           "ORDER BY c.createdAt DESC")
    List<Chat> findBySender(@Param("sender") User sender);

    /**
     * 특정 채팅방의 안 읽은 메시지 조회
     *
     * 사용 시나리오:
     * - 안 읽은 메시지 개수 표시
     * - 읽지 않은 메시지 목록 조회
     *
     * @param chatRoom 채팅방
     * @param currentUser 발신자 (본인이 보낸 메시지 제외)
     * @return 안 읽은 메시지 목록
     */
    @Query("SELECT c FROM Chat c " +
           "LEFT JOIN FETCH c.sender " +
           "WHERE c.chatRoom = :chatRoom " +
           "AND c.isRead = false " +
           "AND c.sender != :currentUser " +
           "ORDER BY c.createdAt ASC")
    List<Chat> findUnreadMessages(@Param("chatRoom") ChatRoom chatRoom,
                                   @Param("currentUser") User currentUser);

    /**
     * 특정 채팅방의 메시지 개수
     *
     * @param chatRoom 채팅방
     * @return 메시지 개수
     */
    @Query("SELECT COUNT(c) FROM Chat c WHERE c.chatRoom = :chatRoom")
    long countByChatRoom(@Param("chatRoom") ChatRoom chatRoom);

    /**
     * 특정 채팅방의 안 읽은 메시지 개수 (본인이 보낸 메시지 제외)
     *
     * @param chatRoom 채팅방
     * @param currentUser 현재 사용자
     * @return 안 읽은 메시지 개수
     */
    @Query("SELECT COUNT(c) FROM Chat c " +
           "WHERE c.chatRoom = :chatRoom " +
           "AND c.isRead = false " +
           "AND c.sender != :currentUser")
    long countUnreadMessages(@Param("chatRoom") ChatRoom chatRoom,
                             @Param("currentUser") User currentUser);

}