package com.example.scsa.dto.response;

import com.example.scsa.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * OAuth2 제공자 (kakao 등)
     */
    private String provider;

    /**
     * 제공자의 사용자 ID
     */
    private String providerId;

    /**
     * 사용자 이름
     */
    private String name;

    /**
     * 프로필 이미지 URL
     */
    private String imageUrl;

    /**
     * 닉네임
     */
    private String nickname;

    /**
     * User Entity로부터 DTO 생성
     */
    public static UserInfoResponse from(User user) {
        return UserInfoResponse.builder()
                .userId(user.getUserId())
                .provider(user.getProvider())
                .providerId(user.getProviderId())
                .name(user.getName())
                .imageUrl(user.getImgUrl())
                .nickname(user.getNickname())
                .build();
    }
}
