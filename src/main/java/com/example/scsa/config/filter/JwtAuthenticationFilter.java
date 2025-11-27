package com.example.scsa.config.filter;

import com.example.scsa.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.HashMap;
import java.util.Map;

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
        } else if (StringUtils.hasText(token) && !jwtUtil.validateToken(token) && isProtectedApi) {
            // 보호된 API에 만료된/유효하지 않은 토큰으로 접근하는 경우 401 반환
            log.warn("인증 실패 - 만료되거나 유효하지 않은 토큰: {}", uri);
            sendUnauthorizedResponse(response, "토큰이 만료되었거나 유효하지 않습니다.");
            return;
        } else if (!StringUtils.hasText(token) && isProtectedApi) {
            // 보호된 API에 토큰 없이 접근하는 경우 401 반환
            log.warn("인증 실패 - 보호된 API에 토큰 없이 접근: {}", uri);
            sendUnauthorizedResponse(response, "인증 토큰이 필요합니다.");
            return;
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
        return uri.startsWith("/api/v1/auth/logout") ||
               uri.startsWith("/api/v1/auth/refresh") ||
               uri.startsWith("/api/v1/matches") ||
               uri.startsWith("/api/v1/tennis-courts") ||
               uri.startsWith("/api/v1/users/check-nickname") ||
               uri.matches("/api/v1/users/\\d+");  // /api/v1/users/{숫자}
    }

    private String resolveToken(HttpServletRequest request) {
        // Authorization 헤더에서 JWT 읽기
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }

        return null;
    }

    /**
     * 401 Unauthorized 응답 전송
     */
    private void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "UNAUTHORIZED");
        errorResponse.put("message", message);
        errorResponse.put("status", 401);

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
