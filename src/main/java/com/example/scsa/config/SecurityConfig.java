package com.example.scsa.config;

import com.example.scsa.config.filter.JwtAuthenticationFilter;
import com.example.scsa.handler.auth.OAuth2LoginFailureHandler;
import com.example.scsa.handler.auth.OAuth2LoginSuccessHandler;
import com.example.scsa.service.auth.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 설정
 * OAuth2 소셜 로그인 + JWT 기반 인증 시스템
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 (프론트엔드와 백엔드가 다른 도메인일 때 필수)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                // JWT 기반 인증 사용으로 CSRF 보호 불필요 (쿠키 미사용)
                .csrf(AbstractHttpConfigurer::disable)
                // HTTP Basic 인증 비활성화
                .httpBasic(AbstractHttpConfigurer::disable)
                // Form 로그인 비활성화 (OAuth2 로그인 사용)
                .formLogin(AbstractHttpConfigurer::disable)
                // JWT 기반 인증 - 세션 완전 비활성화 (stateless)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 경로별 접근 권한 설정
                .authorizeHttpRequests(authorize -> authorize
                        // Public 경로 - 정적 리소스
                        .requestMatchers("/", "/index.html", "/chat-test.html", "/error", "/favicon.ico").permitAll()

                        // OAuth2 로그인 관련 경로 (매우 중요!)
                        .requestMatchers("/login", "/login/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll() // OAuth2 인증 시작
                        .requestMatchers("/api/oauth2/**").permitAll() // 커스텀 OAuth2 경로

                        // OAuth2 콜백 경로
                        .requestMatchers("/auth/**").permitAll()

                        // 인증/인가 API
                        .requestMatchers("/api/v1/auth/status", "/api/v1/auth/logout", "/api/v1/auth/refresh").permitAll()

                        // WebSocket 연결 경로 -> jwt 인터셉터로 대체
//                        .requestMatchers("/ws-stomp/**").permitAll()

                        // Health check 경로
                        .requestMatchers("/internal/health").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        // Swagger UI 경로
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()

                        // Public API (특정 경로가 먼저 와야 함!)
                        .requestMatchers("/api/v1/matches").permitAll()
                        .requestMatchers("/api/v1/tennis-courts/**").permitAll()
                        .requestMatchers("/api/v1/users/**").permitAll()  // 사용자 프로필 조회 공개

                        // Protected API - JWT 인증 필요
                        .requestMatchers("/api/v1/**").hasRole("USER")

                        .anyRequest().permitAll()
                )

                // OAuth2 소셜 로그인 설정
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .redirectionEndpoint(redirect -> redirect.baseUri("/login/oauth2/code/*"))
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2LoginSuccessHandler)  // 로그인 성공 시 JWT 발급
                        .failureHandler(oAuth2LoginFailureHandler)
                )

                // 로그아웃 설정
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                )

                // JWT 인증 필터를 UsernamePasswordAuthenticationFilter 이전에 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
