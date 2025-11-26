package com.example.scsa.config.websocket;

import com.example.scsa.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * WebSocket STOMP 메시지에 대한 JWT 인증 및 권한 검증 인터셉터
 *
 * CONNECT: JWT 토큰 검증 및 인증 정보 설정
 * SUBSCRIBE/SEND: 인증된 사용자만 허용
 * DISCONNECT/HEARTBEAT: 허용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // 1. CONNECT: JWT 토큰 검증 및 인증 정보 설정
        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        }
        // 2. SUBSCRIBE/SEND: 인증된 사용자만 허용
        else if (StompCommand.SUBSCRIBE.equals(command) || StompCommand.SEND.equals(command)) {
            handleAuthenticatedCommand(accessor, command);
        }
        // 3. DISCONNECT/HEARTBEAT: 그대로 허용
        // 추가 처리 불필요

        return message;
    }

    /**
     * CONNECT 시 JWT 검증
     */
    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);

        log.info("=== WebSocket CONNECT 요청 ===");
        log.info("Session ID: {}", accessor.getSessionId());
        log.info("토큰 존재 여부: {}", token != null);

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            try {
                // JWT에서 사용자 정보 추출
                Long userId = jwtUtil.getUserIdFromToken(token);
                String role = "ROLE_USER";

                log.info("WebSocket 인증 성공 - User ID: {}", userId);

                // Spring Security 인증 객체 생성
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority(role))
                );

                // STOMP 세션에 인증 정보 저장
                accessor.setUser(authentication);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("WebSocket 인증 정보 설정 완료");

            } catch (Exception e) {
                log.error("WebSocket JWT 처리 중 오류: {}", e.getMessage());
                throw new IllegalArgumentException("유효하지 않은 JWT 토큰입니다.");
            }
        } else {
            log.warn("WebSocket 연결 실패 - 토큰 없음 또는 유효하지 않음");
            throw new IllegalArgumentException("인증이 필요합니다. JWT 토큰을 제공해주세요.");
        }
    }

    /**
     * SUBSCRIBE/SEND 시 인증 확인
     */
    private void handleAuthenticatedCommand(StompHeaderAccessor accessor, StompCommand command) {
        Authentication authentication = (Authentication) accessor.getUser();

        if (authentication == null || !authentication.isAuthenticated()) {
            log.warn("WebSocket {} 거부 - 인증되지 않은 사용자", command);
            throw new IllegalArgumentException("인증이 필요합니다.");
        }

        log.debug("WebSocket {} 허용 - User ID: {}", command, authentication.getName());
    }

    /**
     * STOMP 헤더에서 JWT 토큰 추출
     *
     * 프론트엔드에서 다음과 같이 전송:
     * - Authorization 헤더: "Bearer {token}"
     * - 또는 token 헤더: "{token}"
     */
    private String extractToken(StompHeaderAccessor accessor) {
        // Authorization 헤더에서 JWT 추출
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
}
