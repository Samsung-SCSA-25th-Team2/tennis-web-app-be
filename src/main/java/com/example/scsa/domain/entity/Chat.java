package com.example.scsa.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 채팅 메시지 엔티티
 * 채팅방에서 주고받는 개별 메시지 정보를 관리
 */
@Entity
@Table(name = "chat")
@EntityListeners(AuditingEntityListener.class) // createdAt 자동 설정을 위한 리스너
@Getter
@NoArgsConstructor
public class Chat {

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

    // 메시지 수신자: 지연 로딩으로 성능 최적화
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    // 메시지 내용
    @Column(nullable = false)
    private String message;

    // 메시지 전송 시간: @CreatedDate로 자동 설정
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 생성자: 채팅 메시지 생성
    public Chat(ChatRoom chatRoom, User sender, User receiver, String message) {
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
    }

    // equals & hashCode: JPA에서 엔티티 동등성 비교를 위해 오버라이드
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return id != null && id.equals(chat.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}
