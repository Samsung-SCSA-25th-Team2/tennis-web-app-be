package com.example.scsa.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 매치 참가자 엔티티
 * Match와 User 간의 다대다 관계를 풀어낸 중간 테이블
 * 한 매치에 여러 유저가 참여할 수 있고, 한 유저는 여러 매치에 참여할 수 있음
 *
 * 설계 참고:
 * - host(호스트)도 이 테이블에 포함됨 (isHost = true)
 * - 일반 참가자는 isHost = false
 * - 이를 통해 전체 참가자를 한 번에 조회 가능
 */
@Entity
@Table(
    name = "match_guest",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_match_guest",
        columnNames = {"match_id", "user_id"} // 같은 매치에 같은 유저가 중복 참여 방지
    )
)
@Getter
@NoArgsConstructor
public class MatchGuest {

    // 기본키: 자동 증가 방식
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_guest_id")
    private Long id;

    // 참여한 매치: 지연 로딩으로 성능 최적화
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    // 참여한 유저: host와 guest 모두 포함
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 호스트 여부: true = 매치 생성자(호스트), false = 일반 참가자
    @Column(nullable = false)
    private Boolean isHost;

    // 생성자: 매치 참가 정보 생성
    public MatchGuest(Match match, User user, Boolean isHost) {
        this.match = match;
        this.user = user;
        this.isHost = isHost;
    }

}
