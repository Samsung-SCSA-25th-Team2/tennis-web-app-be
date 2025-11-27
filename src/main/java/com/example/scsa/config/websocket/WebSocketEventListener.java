package com.example.scsa.config.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;

/**
 * WebSocket 세션 이벤트 리스너
 *
 * 주요 기능:
 * 1. 연결/해제 이벤트 로깅
 * 2. 활성 세션 모니터링
 * 3. 비정상 연결 감지
 *
 * 이벤트 순서:
 * CONNECT → CONNECTED → SUBSCRIBE → (메시지 송수신) → UNSUBSCRIBE → DISCONNECT
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    /**
     * 클라이언트가 연결 시도할 때 발생 (CONNECT 프레임)
     * Heartbeat 설정 및 JWT 검증 전
     */
    @EventListener
    public void handleWebSocketConnectEvent(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        log.info("⚡ WebSocket 연결 시도 - SessionId: {}", sessionId);
    }

    /**
     * 연결이 성공적으로 완료되었을 때 발생 (CONNECTED 프레임)
     * Heartbeat가 활성화되는 시점
     */
    @EventListener
    public void handleWebSocketConnectedEvent(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // Heartbeat 설정 확인
        long[] heartbeat = accessor.getHeartbeat();
        if (heartbeat != null && heartbeat.length == 2) {
            log.info("✅ WebSocket 연결 완료 - SessionId: {}, Heartbeat: 서버→클라이언트={}ms, 클라이언트→서버={}ms",
                    sessionId, heartbeat[0], heartbeat[1]);
        } else {
            log.info("✅ WebSocket 연결 완료 - SessionId: {} (Heartbeat 없음)", sessionId);
        }
    }

    /**
     * 클라이언트가 특정 목적지를 구독할 때 발생 (SUBSCRIBE 프레임)
     * 예: /topic/chatroom.123 구독
     */
    @EventListener
    public void handleWebSocketSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();

        log.info("📬 구독 시작 - SessionId: {}, Destination: {}", sessionId, destination);
    }

    /**
     * 클라이언트가 구독을 취소할 때 발생 (UNSUBSCRIBE 프레임)
     */
    @EventListener
    public void handleWebSocketUnsubscribeEvent(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        log.info("📭 구독 취소 - SessionId: {}", sessionId);
    }

    /**
     * 연결이 종료될 때 발생 (DISCONNECT 프레임 또는 연결 끊김)
     *
     * 종료 원인:
     * 1. 클라이언트가 명시적으로 연결 종료
     * 2. Heartbeat 타임아웃 (응답 없음)
     * 3. 네트워크 오류
     * 4. 서버 종료
     */
    @EventListener
    public void handleWebSocketDisconnectEvent(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        // 종료 원인 확인
        String closeStatus = event.getCloseStatus() != null
                ? event.getCloseStatus().toString()
                : "UNKNOWN";

        log.info("❌ WebSocket 연결 종료 - SessionId: {}, Reason: {}", sessionId, closeStatus);

        // 비정상 종료 감지
        if (event.getCloseStatus() != null && event.getCloseStatus().getCode() != 1000) {
            log.warn("⚠️  비정상 연결 종료 감지 - SessionId: {}, Code: {}, Reason: {}",
                    sessionId,
                    event.getCloseStatus().getCode(),
                    event.getCloseStatus().getReason());
        }
    }
}
