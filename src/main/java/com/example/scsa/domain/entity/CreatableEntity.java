package com.example.scsa.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 생성 시간만 관리하는 엔티티
 * 수정이 발생하지 않는 엔티티(Chat, ChatRoom 등)에서 사용
 *
 * 설계 참고:
 * - @MappedSuperclass: 이 클래스는 테이블로 생성되지 않고, 상속받은 엔티티에 필드만 추가됨
 * - @EntityListeners: JPA Auditing으로 createdAt 자동 설정
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class CreatableEntity {

    // 생성 시간: 엔티티가 처음 저장될 때 자동 설정
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
