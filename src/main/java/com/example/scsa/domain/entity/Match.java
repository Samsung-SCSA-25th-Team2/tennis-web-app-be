package com.example.scsa.domain.entity;

import com.example.scsa.domain.vo.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 테니스 매치 엔티티
 * 테니스 경기 모집/참여를 위한 매치 정보를 관리
 *
 * BaseTimeEntity 상속으로 createdAt, lastModifiedAt 자동 관리
 */
@Entity
@Table(name = "match")
@Getter
@NoArgsConstructor
public class Match extends BaseTimeEntity {

    // 기본키: 자동 증가 방식
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "match_id")
    private Long id;

    // 매치 생성자(호스트): 빠른 접근을 위해 별도 보관
    // 참고: host도 matchGuests 목록에 포함됨 (isHost = true)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    // 양방향 관계: 매치에 참여한 모든 사람 (호스트 + 게스트)
    // cascade: 매치 삭제 시 참가자 정보도 함께 삭제
    // orphanRemoval: 참가자 목록에서 제거 시 DB에서도 삭제
    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MatchGuest> matchGuests = new ArrayList<>();

    // 경기장 정보: Court는 별도 테이블이 아닌 Match 테이블에 함께 저장됨
    @Embedded
    private Court court;

    // 매치 상태: RECRUITING(모집중), COMPLETED(완료됨)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStatus matchStatus;

    // 매치 모집 조건: 희망하는 나이대 (복수 선택 가능)
    // ElementCollection: 별도 테이블(match_age)에 저장
    // LAZY: 필요할 때만 조회하여 성능 최적화
    @ElementCollection(targetClass = Age.class, fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "match_age", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "age")
    private Set<Age> ages = new HashSet<>();

    // 매치 모집 조건: 희망하는 성별 (복수 선택 가능)
    @ElementCollection(targetClass = Gender.class, fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "match_gender", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "gender")
    private Set<Gender> genders = new HashSet<>();

    // 매치 모집 조건: 희망하는 경력 (복수 선택 가능)
    @ElementCollection(targetClass = Period.class, fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "match_period", joinColumns = @JoinColumn(name = "match_id"))
    @Column(name = "period")
    private Set<Period> periods = new HashSet<>();

    // 매치 시작 시간
    @Column(nullable = false)
    private LocalDateTime matchStartDateTime;

    // 매치 종료 시간
    @Column(nullable = false)
    private LocalDateTime matchEndDateTime;

    // 참가비 (비즈니스 로직에서 0원 이상으로 밸리데이션 필요, 무료도 가능)
    @Column(nullable = false)
    private Long fee;

    // 매치 설명 (선택사항)
    private String description;

    // 생성자: 매치 생성 시 필수 정보 입력
    public Match(User host, Court court, MatchStatus matchStatus,
                 LocalDateTime matchStartDateTime, LocalDateTime matchEndDateTime,
                 Long fee, String description) {
        this.host = host;
        this.court = court;
        this.matchStatus = matchStatus;
        this.matchStartDateTime = matchStartDateTime;
        this.matchEndDateTime = matchEndDateTime;
        this.fee = fee;
        this.description = description;
    }

    // 비즈니스 로직: 모집 조건에 나이대 추가
    public void addAge(Age age) {
        this.ages.add(age);
    }

    // 비즈니스 로직: 모집 조건에 성별 추가
    public void addGender(Gender gender) {
        this.genders.add(gender);
    }

    // 비즈니스 로직: 모집 조건에 경력 추가
    public void addPeriod(Period period) {
        this.periods.add(period);
    }

    // 비즈니스 로직: 매치 상태 변경 (모집중 → 완료 등)
    public void updateMatchStatus(MatchStatus newStatus) {
        this.matchStatus = newStatus;
    }

}
