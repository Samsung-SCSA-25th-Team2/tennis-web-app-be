package com.example.scsa.dto.auth;

import java.util.Map;

/**
 * OAuth2 소셜 로그인 사용자 정보 인터페이스
 * 다양한 소셜 로그인 제공자(Kakao, Google 등)의 사용자 정보를 추상화
 */
public interface OAuth2UserInfo {

    /**
     * 제공자 (kakao, google 등)
     */
    String getProvider();

    /**
     * 제공자로부터 받은 고유 ID
     */
    String getProviderId();

    /**
     * 사용자 이름
     */
    String getName();

    /**
     * 프로필 이미지 URL
     */
    String getImageUrl();

    /**
     * 전체 속성 맵
     */
    Map<String, Object> getAttributes();
}