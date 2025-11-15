package com.example.scsa.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 시간 정보 기본 엔티티
 * 생성 시간과 수정 시간을 자동으로 관리하는 추상 클래스
 *
 * 설계 참고:
 * - @MappedSuperclass: 이 클래스는 테이블로 생성되지 않고, 상속받은 엔티티에 필드만 추가됨
 * - @EntityListeners: JPA Auditing으로 createdAt, lastModifiedAt 자동 설정
 * - 수정 이력 추적이 필요한 엔티티(User, Match 등)에서 상속받아 사용
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseTimeEntity {

    // 생성 시간: 엔티티가 처음 저장될 때 자동 설정
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 마지막 수정 시간: 엔티티가 수정될 때마다 자동 갱신
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime lastModifiedAt;

}
