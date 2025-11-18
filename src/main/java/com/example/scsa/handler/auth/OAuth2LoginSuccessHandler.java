package com.example.scsa.handler.auth;

import com.example.scsa.dto.auth.CustomOAuth2User;
import com.example.scsa.service.RefreshTokenService;
import com.example.scsa.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

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

        // Access Token은 쿼리 파라미터로 전달 (프론트엔드에서 localStorage에 저장)
        String targetUrl = "/index.html?accessToken=" + accessToken;
        log.info("리다이렉트 대상: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * Refresh Token을 httpOnly 쿠키로 추가
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);  // JavaScript에서 접근 불가 (XSS 방지)
        cookie.setSecure(false);   // HTTPS only (배포 시 true로 변경)
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);  // 7일 (초 단위)
        response.addCookie(cookie);
        log.info("Refresh Token 쿠키 설정 완료");
    }
}
