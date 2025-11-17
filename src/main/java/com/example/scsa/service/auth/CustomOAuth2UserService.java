package com.example.scsa.service.auth;

import com.example.scsa.dto.auth.CustomOAuth2User;
import com.example.scsa.dto.auth.KakaoOAuth2UserInfo;
import com.example.scsa.dto.auth.OAuth2UserInfo;
import com.example.scsa.domain.entity.User;
import com.example.scsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 커스텀 OAuth2 사용자 서비스
 * OAuth2 로그인 시 사용자 정보를 처리하고 DB에 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. OAuth2 사용자 정보 가져오기
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 제공자 이름 가져오기 (kakao, google 등)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        log.info("OAuth2 Login - Provider: {}", registrationId);

        // 3. OAuth2UserInfo 객체 생성
        OAuth2UserInfo oauth2UserInfo = getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        log.info("OAuth2 User Info - Provider: {}, ProviderId: {}, Name: {}",
                oauth2UserInfo.getProvider(),
                oauth2UserInfo.getProviderId(),
                oauth2UserInfo.getName());

        // 4. 사용자 정보 처리 (DB 조회 or 신규 생성)
        User user = saveOrUpdate(oauth2UserInfo);

        // 5. CustomOAuth2User 반환
        return new CustomOAuth2User(oauth2UserInfo, user.getId());
    }

    /**
     * 제공자에 따라 적절한 OAuth2UserInfo 구현체 반환
     */
    private OAuth2UserInfo getOAuth2UserInfo(String registrationId, java.util.Map<String, Object> attributes) {
        if ("kakao".equals(registrationId)) {
            return new KakaoOAuth2UserInfo(attributes);
        }
        // 추후 다른 소셜 로그인 추가 시 여기에 추가
        // else if ("google".equals(registrationId)) {
        //     return new GoogleOAuth2UserInfo(attributes);
        // }

        throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인입니다: " + registrationId);
    }

    /**
     * 사용자 정보 저장 또는 업데이트
     * - 이미 가입된 사용자: 정보 업데이트
     * - 신규 사용자: 회원가입 처리
     */
    private User saveOrUpdate(OAuth2UserInfo oauth2UserInfo) {
        User user = userRepository.findByProviderAndProviderId(
                oauth2UserInfo.getProvider(),
                oauth2UserInfo.getProviderId()
        ).map(existingUser -> updateExistingUser(existingUser, oauth2UserInfo))
         .orElseGet(() -> createNewUser(oauth2UserInfo));

        return userRepository.save(user);
    }

    /**
     * 기존 사용자 정보 업데이트
     */
    private User updateExistingUser(User user, OAuth2UserInfo oauth2UserInfo) {
        log.info("기존 사용자 정보 업데이트 - UserId: {}", user.getId());
        user.updateProfile(null, oauth2UserInfo.getImageUrl());
        return user;
    }

    /**
     * 신규 사용자 생성
     * 주의: gender, period, age는 나중에 추가 정보 입력 페이지에서 받아야 함
     */
    private User createNewUser(OAuth2UserInfo oauth2UserInfo) {
        log.info("신규 사용자 생성 - Provider: {}, ProviderId: {}",
                oauth2UserInfo.getProvider(),
                oauth2UserInfo.getProviderId());

        // 닉네임 중복 체크 및 생성
        String nickname = generateUniqueNickname(oauth2UserInfo.getName());

        return User.builder()
                .provider(oauth2UserInfo.getProvider())
                .providerId(oauth2UserInfo.getProviderId())
                .name(oauth2UserInfo.getName())
                .imgUrl(oauth2UserInfo.getImageUrl())
                .nickname(nickname)
                // gender, period, age는 null로 설정 (추후 입력 필요)
                .build();
    }

    /**
     * 중복되지 않는 닉네임 생성
     */
    private String generateUniqueNickname(String baseName) {
        if (baseName == null || baseName.trim().isEmpty()) {
            baseName = "사용자";
        }

        String nickname = baseName;
        int suffix = 1;

        while (userRepository.existsByNickname(nickname)) {
            nickname = baseName + suffix++;
        }

        return nickname;
    }
}