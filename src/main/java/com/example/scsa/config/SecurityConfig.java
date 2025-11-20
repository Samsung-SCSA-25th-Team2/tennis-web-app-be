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

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
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
                        // Public 경로
                        .requestMatchers("/", "/index.html", "/oauth2/**", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/api/v1/auth/status", "/api/v1/auth/logout").permitAll() // 인증 상태 조회 및 로그아웃은 누구나 가능
                        // Swagger UI 경로
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-resources/**").permitAll()
                        // Protected API - JWT 인증 필요
                        .requestMatchers("/api/v1/auth/me").hasRole("USER")
                        .requestMatchers("/api/v1/matches").permitAll()
                        // 나머지는 모두 허용 (개발 중)
                        .anyRequest().authenticated()
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
