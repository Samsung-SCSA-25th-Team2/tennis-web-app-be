package com.example.scsa.dto.response;

import com.example.scsa.domain.entity.User;
import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Gender;
import com.example.scsa.domain.vo.Period;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 프로필 완성 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class ProfileCompleteResponse {

    private Long userId;
    private String nickname;
    private Gender gender;
    private Period period;
    private Age age;
    private String name;
    private String imgUrl;
    private boolean isProfileComplete;
    private String accessToken;  // 새로운 JWT 토큰

    public static ProfileCompleteResponse from(User user, String accessToken) {
        return ProfileCompleteResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .gender(user.getGender())
                .period(user.getPeriod())
                .age(user.getAge())
                .name(user.getName())
                .imgUrl(user.getImgUrl())
                .isProfileComplete(user.isProfileComplete())
                .accessToken(accessToken)
                .build();
    }
}
