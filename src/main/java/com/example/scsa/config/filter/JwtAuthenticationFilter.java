package com.example.scsa.config.filter;

import com.example.scsa.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 헬스체크, 정적 리소스, 공개 엔드포인트는 로깅 최소화
        if (shouldSkipDetailedLogging(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        // 토큰이 있는 요청이나 보호된 API만 상세 로깅
        boolean isProtectedApi = uri.startsWith("/api/v1/") && !isPublicEndpoint(uri);
        if (token != null || isProtectedApi) {
            log.info("=== JWT 필터 ===");
            log.info("요청 URI: {}", uri);
            log.info("토큰 존재 여부: {}", token != null);
            if (token != null) {
                log.debug("토큰 미리보기: {}...", token.substring(0, Math.min(20, token.length())));
            }
        }

        if (StringUtils.hasText(token) && jwtUtil.validateToken(token)) {
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId.toString());

                if (userDetails != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("인증 성공 - 사용자 ID: {}", userId);
                }
            } catch (Exception e) {
                // JWT는 유효하지만 사용자를 찾을 수 없는 경우 (DB 초기화 등)
                log.warn("JWT 토큰은 유효하지만 사용자를 찾을 수 없습니다: {}", e.getMessage());
            }
        } else if (isProtectedApi) {
            // 보호된 API에 토큰 없이 접근하는 경우만 로깅
            log.warn("인증 실패 - 보호된 API에 토큰 없이 접근: {}", uri);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 상세 로깅을 건너뛸 URI 판단
     */
    private boolean shouldSkipDetailedLogging(String uri) {
        return uri.startsWith("/actuator") ||
               uri.startsWith("/internal/health") ||
               uri.equals("/") ||
               uri.equals("/favicon.ico") ||
               uri.equals("/login") ||
               uri.startsWith("/swagger-ui") ||
               uri.startsWith("/v3/api-docs") ||
               uri.startsWith("/oauth2/") ||
               uri.startsWith("/error") ||
               uri.startsWith("/dns/") ||  // 봇 트래픽
               uri.startsWith("/geoserver/") ||  // 봇 트래픽
               uri.contains("/.well-known/") ||  // 봇 트래픽
               uri.endsWith(".ico") ||
               uri.endsWith(".html");
    }

    /**
     * 공개 엔드포인트 판단
     */
    private boolean isPublicEndpoint(String uri) {
        return uri.startsWith("/api/v1/auth/status") ||
               uri.startsWith("/api/v1/auth/logout") ||
               uri.startsWith("/api/v1/auth/refresh") ||
               uri.startsWith("/api/v1/matches") ||
               uri.startsWith("/api/v1/tennis-courts");
    }

    private String resolveToken(HttpServletRequest request) {
        // Authorization 헤더에서 JWT 읽기
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }
}
