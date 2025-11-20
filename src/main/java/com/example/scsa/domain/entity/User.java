package com.example.scsa.domain.entity;

import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Gender;
import com.example.scsa.domain.vo.Period;
import com.example.scsa.domain.vo.Role;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    // OAuth2 로그인 직후에는 null, /complete-profile에서 입력
    @Column(unique = true)
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Gender gender;  // OAuth2 로그인 시 null 가능 (추후 프로필 완성 단계에서 입력)

    @Enumerated(EnumType.STRING)
    private Period period;  // OAuth2 로그인 시 null 가능 (추후 프로필 완성 단계에서 입력)

    @Enumerated(EnumType.STRING)
    private Age age;  // OAuth2 로그인 시 null 가능 (추후 프로필 완성 단계에서 입력)

    private String name;
    private String imgUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;  // 사용자 권한 (기본값: USER)

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false, unique = true)
    private String providerId;


    @Builder
    public User(String nickname, Gender gender, Period period, Age age, String name, String imgUrl, Role role, String provider, String providerId) {
        // OAuth2 로그인 시에는 nickname이 null일 수 있음 (추후 /complete-profile에서 입력)
        if (nickname != null) {
            validateNickname(nickname);
        }
        this.nickname = nickname;
        this.gender = gender;
        this.period = period;
        this.age = age;
        this.name = name;
        this.imgUrl = imgUrl;
        this.role = (role != null) ? role : Role.USER;  // 기본값 설정
        this.provider = provider;
        this.providerId = providerId;
    }

    public static User createOAuth2User(String provider, String providerId, String name, String imgUrl) {

        /**
         * // 필수값이 아닌 값들에 대해 임시 기본값 설정
         *         Gender tempGender = Gender.OTHER; // 혹은 다른 기본값
         *         Period tempPeriod = Period.UNKNOWN; // 혹은 다른 기본값
         *         Age tempAge = Age.UNKNOWN; // 혹은 다른 기본값
         */

        return User.builder()
                .provider(provider)
                .providerId(providerId)
                .name(name)
                .imgUrl(imgUrl)
//                .nickname(nickname)
//                .gender(tempGender)
//                .period(tempPeriod)
//                .age(tempAge)
                .build();
    }
    
    // nickname의 getter
    public String getNickname() {
        return this.nickname;
    }

    private void validateNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("닉네임은 필수입니다.");
        }
        if (nickname.length() > 20) {
            throw new IllegalArgumentException("닉네임은 20자 이하여야 합니다.");
        }
    }

    public void updateProfile(String nickname, String imgUrl) {
        if (nickname != null) {
            validateNickname(nickname);
            this.nickname = nickname;
        }
        if (imgUrl != null) {
            this.imgUrl = imgUrl;
        }
    }

    public void updatePeriod(Period period) {
        this.period = period;
    }

    public void updateAge(Age age) {
        this.age = age;
    }

    public void updateGender(Gender gender) {
        this.gender = gender;
    }

    // 비즈니스 로직: 프로필 완성 여부 확인
    public boolean isProfileComplete() {
        return this.nickname != null
            && this.gender != null
            && this.period != null
            && this.age != null;
    }

    // 비즈니스 로직: 프로필 완성 (회원가입 폼 제출)
    public void completeProfile(String nickname, Gender gender, Period period, Age age) {
        validateNickname(nickname);
        this.nickname = nickname;
        this.gender = gender;
        this.period = period;
        this.age = age;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return id != null && id.equals(user.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : getClass().hashCode();
    }
    
    public Long getUserId() {
        return this.id;
    }

}
