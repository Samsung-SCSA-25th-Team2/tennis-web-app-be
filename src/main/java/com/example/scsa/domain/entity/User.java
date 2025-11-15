package com.example.scsa.domain.entity;

import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Gender;
import com.example.scsa.domain.vo.Period;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 사용자 엔티티
 * 테니스 매칭 서비스를 이용하는 회원 정보를 관리
 *
 * BaseTimeEntity 상속으로 createdAt, lastModifiedAt 자동 관리
 */
@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor // JPA에서 프록시 객체 생성을 위해 필수
public class User extends BaseTimeEntity {

    // 기본키: 자동 증가 방식
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    // 양방향 관계: 이 사용자가 참여한 매치 목록 (host + guest 모두 포함)
    // mappedBy: MatchGuest 엔티티의 'user' 필드에 의해 관리됨
    @OneToMany(mappedBy = "user")
    private List<MatchGuest> matchGuests = new ArrayList<>();

    // 닉네임: 중복 불가, 필수 입력
    @Column(nullable = false, unique = true)
    private String nickname;

    // 성별: enum을 문자열로 저장 (MALE, FEMALE, OTHER)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    // 테니스 경력: enum을 문자열로 저장 (ONE_YEAR, TWO_YEARS 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Period period;

    // 나이대: enum을 문자열로 저장 (TWENTY, THIRTY 등)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Age age;

    // 카카오 소셜 로그인으로부터 받는 정보
    private String name;    // 카카오 프로필 이름
    private String imgUrl;  // 카카오 프로필 이미지 URL

    // 생성자: 필수 정보를 받아 User 객체 생성
    public User(String nickname, Gender gender, Period period, Age age, String name, String imgUrl) {
        this.nickname = nickname;
        this.gender = gender;
        this.period = period;
        this.age = age;
        this.name = name;
        this.imgUrl = imgUrl;
    }

}
