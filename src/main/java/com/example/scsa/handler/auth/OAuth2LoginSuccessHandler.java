package com.example.scsa.handler.auth;

import com.example.scsa.dto.auth.CustomOAuth2User;
import com.example.scsa.util.JwtUtil;
import jakarta.servlet.ServletException;
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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        log.info("=== OAuth2LoginSuccessHandler 호출됨 ===");
        log.info("Authentication Principal Type: {}", authentication.getPrincipal().getClass().getName());

        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();

        Long userId = oauth2User.getUserId();
        String role = "ROLE_USER";

        log.info("JWT 생성 시작 - UserId: {}, Role: {}", userId, role);
        String token = jwtUtil.generateToken(userId, role);
        log.info("JWT 생성 완료 - Token length: {}", token.length());

        // JWT를 쿼리 파라미터로 전달 (프론트엔드에서 localStorage에 저장)
        String targetUrl = determineTargetUrl(request, response, authentication, token);
        log.info("리다이렉트 대상: {}", targetUrl);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication, String token) {
        // 로그인 성공 후 메인 페이지로 리다이렉트하면서 JWT를 쿼리 파라미터로 전달
        // 프론트엔드에서 토큰을 받아 localStorage에 저장
        return "/index.html?token=" + token;
    }
}
