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
 * - host(호스트) 여부는 Match.host_id와 비교하여 판단 (match.host.id == user.id)
 * - is_host 필드를 제거하여 데이터 중복 및 불일치 방지
 * - 전체 참가자(호스트 포함)를 한 번에 조회 가능
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

    // 생성자: 매치 참가 정보 생성
    public MatchGuest(Match match, User user) {
        this.match = match;
        this.user = user;
    }

    // 비즈니스 로직: 이 참가자가 호스트인지 확인
    public boolean isHost() {
        return match.getHost().equals(user);
    }

    // equals & hashCode: JPA에서 엔티티 동등성 비교를 위해 오버라이드
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MatchGuest)) return false;
        MatchGuest that = (MatchGuest) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }

}
