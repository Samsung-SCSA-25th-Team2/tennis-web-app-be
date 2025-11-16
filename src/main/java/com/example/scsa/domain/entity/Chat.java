package com.example.scsa.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 엔티티
 * 채팅방에서 주고받는 개별 메시지 정보를 관리
 *
 * 설계 참고:
 * - receiver는 별도로 저장하지 않음 (ChatRoom의 Match를 통해 참가자 확인 가능)
 * - CreatableEntity 상속으로 createdAt 자동 관리
 */
@Entity
@Table(name = "chat")
@Getter
@NoArgsConstructor
public class Chat extends CreatableEntity {

    // 기본키: 자동 증가 방식
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private Long id;

    /**
     * 소속 채팅방: 이 메시지가 어느 채팅방에 속하는지 저장
     *
     * 설계 참고:
     * - Long roomId 대신 ChatRoom 엔티티를 직접 참조
     * - JPA가 엔티티 수준에서 관리하여 객체지향적 설계 가능
     * - 연관 객체 조회, JPQL 작성 등에서 편의성 향상
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    // 메시지 발신자: 지연 로딩으로 성능 최적화
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    // 메시지 내용
    @Column(nullable = false, length = 500)
    private String message;

    // 읽음 여부: 기본값 false (안 읽음)
    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    // 읽은 시간: 메시지를 읽었을 때 설정
    @Column(name = "read_at")
    private LocalDateTime readAt;

    // 생성자: 채팅 메시지 생성
    public Chat(ChatRoom chatRoom, User sender, String message) {
        validateMessage(message);

        this.chatRoom = chatRoom;
        this.sender = sender;
        this.message = message;
        this.isRead = false;
    }

    // 메시지 검증
    private void validateMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 필수입니다.");
        }
        if (message.length() > 500) {
            throw new IllegalArgumentException("메시지는 500자 이하여야 합니다.");
        }
    }

    // 비즈니스 로직: 메시지 읽음 처리
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    // 비즈니스 로직: 특정 사용자가 읽을 수 있는 메시지인지 확인
    public boolean canBeReadBy(User user) {
        // 발신자가 아닌 사용자만 읽음 처리 가능
        return !this.sender.equals(user);
    }

    // equals & hashCode: JPA에서 엔티티 동등성 비교를 위해 오버라이드
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Chat)) return false;
        Chat chat = (Chat) o;
        return id != null && id.equals(chat.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }

}
