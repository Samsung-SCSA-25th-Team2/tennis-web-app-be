package com.example.scsa.handler.auth;

import com.example.scsa.dto.auth.CustomOAuth2User;
import com.example.scsa.service.RefreshTokenService;
import com.example.scsa.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    // 프론트엔드 URL (환경별로 다름)
    @Value("${app.frontend-url}")
    private String frontendUrl;

    // 쿠키 설정
    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    @Value("${cookie.domain:}")
    private String cookieDomain;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("=== OAuth2LoginSuccessHandler 호출됨 ===");
        log.info("Authentication Principal Type: {}", authentication.getPrincipal().getClass().getName());

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        Long userId = oauth2User.getUserId();
        String role = "ROLE_USER";

        log.info("JWT 생성 시작 - UserId: {}, Role: {}", userId, role);

        // Access Token + Refresh Token 생성
        String accessToken = jwtUtil.generateAccessToken(userId, role);
        String refreshToken = jwtUtil.generateRefreshToken(userId);

        log.info("Access Token 생성 완료 - Token length: {}", accessToken.length());
        log.info("Refresh Token 생성 완료 - Token length: {}", refreshToken.length());

        // Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(userId, refreshToken, jwtUtil.getRefreshTokenExpiration());

        // Refresh Token을 httpOnly 쿠키로 설정
        addRefreshTokenCookie(response, refreshToken);

        // 프론트엔드 URL로 리다이렉트 (Access Token을 쿼리 파라미터로 전달)
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                .path("/auth/callback")  // 프론트엔드의 OAuth 콜백 경로
                .queryParam("accessToken", accessToken)
                .build()
                .toUriString();

        log.info("리다이렉트 대상: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Refresh Token을 httpOnly 쿠키로 추가
     * 환경별 설정값(secure, sameSite, domain)을 적용
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)  // JavaScript에서 접근 불가 (XSS 방지)
                .secure(cookieSecure)  // 환경별 설정: 로컬(false), 운영(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)  // 7일 (초 단위)
                .sameSite(cookieSameSite)  // 환경별 설정: 로컬(Lax), 운영(None)
                .domain(cookieDomain.isEmpty() ? null : cookieDomain)  // 환경별 도메인 설정
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        log.info("Refresh Token 쿠키 설정 완료 - Secure: {}, SameSite: {}, Domain: {}",
                 cookieSecure, cookieSameSite, cookieDomain.isEmpty() ? "미설정" : cookieDomain);
    }
}
