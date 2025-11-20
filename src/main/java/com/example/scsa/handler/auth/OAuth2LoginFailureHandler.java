package com.example.scsa.handler.auth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OAuth2 로그인 실패 핸들러
 * 로그인 실패 시 수행할 작업을 정의
 */
@Slf4j
@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.error("OAuth2 로그인 실패 - Error: {}", exception.getMessage());

        // TODO: 프론트엔드 로그인 실패 페이지로 리다이렉트
        String targetUrl = "/login?error=true";
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}