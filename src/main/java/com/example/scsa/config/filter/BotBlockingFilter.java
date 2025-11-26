package com.example.scsa.config.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * 악의적인 봇 트래픽 및 스캐너를 차단하는 필터
 * 일반적인 공격 경로에 대한 요청을 조기 차단하여 서버 부하 감소
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BotBlockingFilter extends OncePerRequestFilter {

    // 차단할 경로 패턴 (일반적인 스캔/공격 경로)
    private static final Set<String> BLOCKED_PATHS = Set.of(
            "/dns/",
            "/geoserver/",
            "/admin/",
            "/phpmyadmin/",
            "/wp-admin/",
            "/wp-login.php",
            "/.env",
            "/.git/",
            "/config/",
            "/api/config/",
            "/console/",
            "/solr/",
            "/jenkins/",
            "/manager/",
            "/actuator/env",
            "/actuator/beans",
            "/actuator/heapdump",
            "/phpinfo.php",
            "/shell.php",
            "/test.php"
    );

    // 차단할 파일 확장자
    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(
            ".php",
            ".asp",
            ".aspx",
            ".cgi",
            ".jsp"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI().toLowerCase();
        String originalUri = request.getRequestURI(); // 대소문자 구분이 필요한 경우
        String method = request.getMethod();

        // OAuth2 로그인 경로는 절대 차단하지 않음
        if (originalUri.startsWith("/oauth2/") ||
            originalUri.startsWith("/api/oauth2/") ||
            originalUri.startsWith("/login/oauth2/") ||
            originalUri.startsWith("/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 의심스러운 경로 차단
        for (String blockedPath : BLOCKED_PATHS) {
            if (uri.contains(blockedPath.toLowerCase())) {
                log.warn("봇 트래픽 차단 - Path: {}, IP: {}, User-Agent: {}",
                        originalUri,
                        getClientIp(request),
                        request.getHeader("User-Agent"));
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
        }

        // 의심스러운 파일 확장자 차단 (단, API 엔드포인트는 제외)
        if (!uri.startsWith("/api/")) {
            for (String blockedExt : BLOCKED_EXTENSIONS) {
                if (uri.endsWith(blockedExt)) {
                    log.warn("봇 트래픽 차단 - Extension: {}, IP: {}", uri, getClientIp(request));
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }
        }

        // SQL Injection 시도 감지
        String queryString = request.getQueryString();
        if (queryString != null && containsSqlInjection(queryString)) {
            log.warn("SQL Injection 시도 차단 - Query: {}, IP: {}", queryString, getClientIp(request));
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        // 비정상적으로 긴 URI 차단 (DoS 공격 방지)
        if (uri.length() > 2000) {
            log.warn("비정상적으로 긴 URI 차단 - Length: {}, IP: {}", uri.length(), getClientIp(request));
            response.setStatus(414); // URI Too Long
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 클라이언트 실제 IP 주소 추출 (프록시/로드밸런서 고려)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For에 여러 IP가 있는 경우 첫 번째 IP 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * SQL Injection 패턴 감지
     */
    private boolean containsSqlInjection(String input) {
        String lowerInput = input.toLowerCase();
        return lowerInput.contains("union select") ||
               lowerInput.contains("drop table") ||
               lowerInput.contains("insert into") ||
               lowerInput.contains("delete from") ||
               lowerInput.contains("' or '1'='1") ||
               lowerInput.contains("'; --") ||
               lowerInput.contains("or 1=1") ||
               lowerInput.contains("<script") ||
               lowerInput.contains("javascript:");
    }
}