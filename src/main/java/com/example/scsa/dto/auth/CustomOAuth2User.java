package com.example.scsa.dto.auth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 커스텀 OAuth2 사용자 정보
 * Spring Security의 OAuth2User를 구현하면서 우리 애플리케이션에 필요한 정보를 추가
 */
@Getter
public class CustomOAuth2User implements OAuth2User {

    private final OAuth2UserInfo oauth2UserInfo;
    private final Long userId;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomOAuth2User(OAuth2UserInfo oauth2UserInfo, Long userId) {
        this.oauth2UserInfo = oauth2UserInfo;
        this.userId = userId;
        // 모든 OAuth2 사용자에게 USER 권한 부여
        this.authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2UserInfo.getAttributes();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return oauth2UserInfo.getProviderId();
    }

    public String getProvider() {
        return oauth2UserInfo.getProvider();
    }

    public String getProviderId() {
        return oauth2UserInfo.getProviderId();
    }

    public String getDisplayName() {
        return oauth2UserInfo.getName();
    }

    public String getImageUrl() {
        return oauth2UserInfo.getImageUrl();
    }
}