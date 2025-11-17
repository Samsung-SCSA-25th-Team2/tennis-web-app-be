package com.example.scsa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 상태 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthStatusResponse {

    /**
     * 인증 여부
     */
    private boolean authenticated;

    /**
     * 사용자 ID (인증된 경우)
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
     * 응답 메시지
     */
    private String message;

    /**
     * 로그인 URL (미인증 시)
     */
    private String loginUrl;

    /**
     * 미인증 사용자 응답 생성
     */
    public static AuthStatusResponse unauthenticated() {
        return AuthStatusResponse.builder()
                .authenticated(false)
                .message("로그인이 필요합니다.")
                .loginUrl("/oauth2/authorization/kakao")
                .build();
    }

    /**
     * 인증된 사용자 응답 생성
     */
    public static AuthStatusResponse authenticated(Long userId, String provider, String providerId,
                                                     String name, String imageUrl) {
        return AuthStatusResponse.builder()
                .authenticated(true)
                .userId(userId)
                .provider(provider)
                .providerId(providerId)
                .name(name)
                .imageUrl(imageUrl)
                .message("로그인 성공!")
                .build();
    }
}
