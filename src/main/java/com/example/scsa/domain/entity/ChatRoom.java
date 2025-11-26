package com.example.scsa.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 채팅방 엔티티
 * 매치 내에서 두 유저 간의 1:1 채팅방을 관리
 *
 * 설계 참고:
 * - 하나의 매치에서 여러 채팅방 생성 가능 (호스트-게스트1, 호스트-게스트2, ...)
 * - user1, user2: 채팅방의 두 참여자 (순서 무관)
 * - matchId: 어느 매치에서 만났는지 기록 (느슨한 결합)
 * - unique constraint: 같은 매치에서 같은 두 유저 간 채팅방 중복 방지
 * - CreatableEntity 상속으로 createdAt 자동 관리
 */
@Entity
@Table(
    name = "chat_room",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_chat_room_match_users",
        columnNames = {"match_id", "user1_id", "user2_id"}
    )
)
@Getter
@NoArgsConstructor
public class ChatRoom extends CreatableEntity {

    // 기본키: 자동 증가 방식
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    // 연관된 매치 ID: 어느 매치에서 만났는지 (느슨한 결합)
    @Column(name = "match_id", nullable = false)
    private Long matchId;

    // 채팅방 참여자 1: 보통 호스트 (순서는 중요하지 않음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;

    // 채팅방 참여자 2: 보통 게스트 (순서는 중요하지 않음)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;

    // 양방향 관계: 이 채팅방에 속한 메시지 목록
    // cascade: 채팅방 삭제 시 메시지도 함께 삭제
    // orphanRemoval: 메시지 목록에서 제거 시 DB에서도 삭제
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chat> chats = new ArrayList<>();

    // 마지막 메시지 시간: 채팅방 목록 정렬에 사용
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    // 마지막 메시지 미리보기: UI에서 표시용
    @Column(name = "last_message_preview", length = 100)
    private String lastMessagePreview;

    // 생성자: 채팅방 생성 (매치 기반)
    public ChatRoom(Long matchId, User user1, User user2) {
        validateUsers(user1, user2);
        if (matchId == null) {
            throw new IllegalArgumentException("매치 ID는 필수입니다.");
        }
        this.matchId = matchId;
        this.user1 = user1;
        this.user2 = user2;
    }

    // 유저 검증
    private void validateUsers(User user1, User user2) {
        if (user1 == null || user2 == null) {
            throw new IllegalArgumentException("채팅방 참여자는 필수입니다.");
        }
        if (user1.equals(user2)) {
            throw new IllegalArgumentException("같은 사용자끼리 채팅방을 만들 수 없습니다.");
        }
    }

    // 양방향 연관관계 편의 메서드: 채팅 메시지 추가
    public void addChat(Chat chat) {
        this.chats.add(chat);
        updateLastMessage(chat);
    }

    // 비즈니스 로직: 마지막 메시지 정보 업데이트
    public void updateLastMessage(Chat chat) {
        this.lastMessageAt = chat.getCreatedAt();
        // 메시지가 너무 길면 100자로 자르기
        String message = chat.getMessage();
        this.lastMessagePreview = message.length() > 100
            ? message.substring(0, 100)
            : message;
    }

    // 비즈니스 로직: 특정 유저의 상대방 조회
    public User getOtherUser(User user) {
        if (user.equals(user1)) {
            return user2;
        } else if (user.equals(user2)) {
            return user1;
        }
        throw new IllegalArgumentException("해당 유저는 이 채팅방의 참여자가 아닙니다.");
    }

    // 비즈니스 로직: 특정 유저가 이 채팅방의 참여자인지 확인
    public boolean isParticipant(User user) {
        return user.equals(user1) || user.equals(user2);
    }

    // 비즈니스 로직: 채팅방의 모든 참가자 조회
    public List<User> getParticipants() {
        return List.of(user1, user2);
    }

    // equals & hashCode: JPA에서 엔티티 동등성 비교를 위해 오버라이드
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatRoom)) return false;
        ChatRoom chatRoom = (ChatRoom) o;
        return id != null && id.equals(chatRoom.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }

    public void updateLastMessage(String message, LocalDateTime sentAt) {
        if (message == null) {
            this.lastMessagePreview = null;
            this.lastMessageAt = sentAt;
            return;
        }

        // 프리뷰 길이 제한 (예: 50자)
        int maxLength = 50;
        this.lastMessagePreview =
                message.length() > maxLength
                        ? message.substring(0, maxLength)
                        : message;

        this.lastMessageAt = sentAt;
    }
}
