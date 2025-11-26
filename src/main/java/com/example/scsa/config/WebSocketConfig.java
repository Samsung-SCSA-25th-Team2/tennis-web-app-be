package com.example.scsa.config;

import com.example.scsa.config.websocket.JwtChannelInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtChannelInterceptor jwtChannelInterceptor;

    @Value("${spring.rabbitmq.host}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.stomp-port}")
    private int rabbitmqStompPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    @Value("${spring.rabbitmq.virtual-host}")
    private String rabbitmqVirtualHost;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 엔드포인트 등록
        // HTTPS 환경에서는 자동으로 WSS(WebSocket Secure) 프로토콜 사용
        log.info("WebSocket 엔드포인트 등록: /ws-stomp");
        log.info("허용된 Origin: {}", allowedOrigins);

        registry.addEndpoint("/ws-stomp")
                // 개발: 모든 Origin 허용, 운영: 특정 도메인만 허용
                .setAllowedOrigins(allowedOrigins.split(","))
                .withSockJS();  // SockJS fallback 지원

        // Native WebSocket 연결도 지원 (SockJS 없이)
        registry.addEndpoint("/ws-stomp")
                .setAllowedOrigins(allowedOrigins.split(","));

        log.info("WSS(WebSocket Secure) 지원: HTTPS 환경에서 자동 활성화");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // RabbitMQ STOMP Broker Relay 사용
        // 여러 서버 인스턴스가 동일한 RabbitMQ 브로커를 공유하여 메시지 동기화
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(rabbitmqHost)
                .setRelayPort(rabbitmqStompPort)
                .setClientLogin(rabbitmqUsername)
                .setClientPasscode(rabbitmqPassword)
                .setSystemLogin(rabbitmqUsername)
                .setSystemPasscode(rabbitmqPassword)
                .setVirtualHost(rabbitmqVirtualHost);

        // 클라이언트가 서버로 메시지를 보낼 때 사용할 프리픽스를 설정합니다.
        // @MessageMapping 어노테이션이 붙은 메소드가 이 프리픽스가 붙은 경로로 들어오는 메시지를 처리합니다.
        registry.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 인바운드 채널에 JWT 인증 인터셉터 등록
     * 클라이언트 → 서버로 오는 모든 STOMP 메시지를 가로채서 JWT 검증
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(jwtChannelInterceptor);
        log.info("JWT Channel Interceptor 등록 완료 - WebSocket 메시지 인증 활성화");
    }
}