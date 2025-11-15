package com.example.scsa.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 채팅방 엔티티
 * 1:1 채팅방을 관리 (host와 guest 간의 채팅방)
 */
@Entity
@EntityListeners(AuditingEntityListener.class) // createdAt 자동 설정을 위한 리스너
@Table(name = "chat_room")
@Getter
@NoArgsConstructor
public class ChatRoom {

    // 기본키: 자동 증가 방식
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_room_id")
    private Long id;

    // 채팅방 개설자: 지연 로딩으로 성능 최적화
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    // 채팅 상대방: 지연 로딩으로 성능 최적화
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private User guest;

    // 양방향 관계: 이 채팅방에 속한 메시지 목록
    // cascade: 채팅방 삭제 시 메시지도 함께 삭제
    // orphanRemoval: 메시지 목록에서 제거 시 DB에서도 삭제
    @OneToMany(mappedBy = "chatRoom", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chat> chats = new ArrayList<>();

    // 채팅방 생성 시간: @CreatedDate로 자동 설정
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 생성자: 채팅방 생성 (host와 guest 정보 필수)
    public ChatRoom(User host, User guest) {
        this.host = host;
        this.guest = guest;
    }

}
