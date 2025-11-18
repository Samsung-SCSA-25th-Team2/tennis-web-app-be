package com.example.scsa.controller;

import com.example.scsa.domain.entity.User;
import com.example.scsa.dto.request.ProfileCompleteRequest;
import com.example.scsa.dto.response.AuthStatusResponse;
import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.dto.response.LogoutResponse;
import com.example.scsa.dto.response.ProfileCompleteResponse;
import com.example.scsa.dto.response.TokenResponse;
import com.example.scsa.dto.response.UserInfoResponse;
import com.example.scsa.repository.UserRepository;
import com.example.scsa.service.RefreshTokenService;
import com.example.scsa.service.UserService;
import com.example.scsa.util.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.authentication.AnonymousAuthenticationToken;

import java.util.HashMap;
import java.util.Map;


/**
 * 인증 관련 컨트롤러
 * JWT 기반 인증 및 사용자 정보 조회 API
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    /**
     * 인증 상태 조회 API
     *
     * @return 인증 상태 정보
     */
    @GetMapping("/status")
    public ResponseEntity<AuthStatusResponse> getAuthStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("=== AuthController.getAuthStatus() ===");
        log.info("Authentication 존재: {}", authentication != null);

        if (authentication != null) {
            log.info("Authentication type: {}", authentication.getClass().getName());
            log.info("Authentication.isAuthenticated(): {}", authentication.isAuthenticated());
            log.info("Is AnonymousAuthenticationToken: {}", authentication instanceof AnonymousAuthenticationToken);
            log.info("Authentication.getName(): {}", authentication.getName());
        }

        // AnonymousAuthenticationToken 체크 추가
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {

            String userIdStr = authentication.getName();
            log.info("인증된 사용자 ID: {}", userIdStr);

            try {
                Long userId = Long.parseLong(userIdStr);
                User user = userRepository.findById(userId).orElse(null);

                if (user != null) {
                    log.info("사용자 정보 조회 성공: {} (ID: {}), 프로필 완성 여부: {}",
                            user.getName(), user.getUserId(), user.isProfileComplete());
                    return ResponseEntity.ok(AuthStatusResponse.authenticated(
                            user.getUserId(),
                            user.getProvider(),
                            user.getProviderId(),
                            user.getName(),
                            user.getImgUrl(),
                            user.isProfileComplete()
                    ));
                } else {
                    log.warn("DB에서 사용자를 찾을 수 없음: userId={}", userId);
                }
            } catch (NumberFormatException e) {
                log.error("잘못된 사용자 ID 형식: {}", userIdStr);
            }
        }

        log.info("미인증 사용자 응답 반환");
        return ResponseEntity.ok(AuthStatusResponse.unauthenticated());
    }


    /**
     * 현재 로그인한 사용자 정보 조회
     *
     * @return 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        String userId = authentication.getName();

        try {
            User user = userRepository.findById(Long.parseLong(userId)).orElse(null);

            if (user != null) {
                UserInfoResponse response = UserInfoResponse.from(user);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패: {}", e.getMessage());
        }

        return ResponseEntity.status(404)
                .body(ErrorResponse.of("사용자를 찾을 수 없습니다.", "USER_NOT_FOUND"));
    }

    /**
     * 프로필 완성 API
     * 카카오 OAuth2 로그인 후 추가 정보(nickname, gender, period, age) 입력
     *
     * @param request 프로필 완성 요청
     * @return 프로필 완성 응답 (새 JWT 토큰 포함)
     */
    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@Valid @RequestBody ProfileCompleteRequest request,
                                              HttpServletResponse httpServletResponse) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        try {
            Long userId = Long.parseLong(authentication.getName());
            ProfileCompleteResponse response = userService.completeProfile(userId, request);

            // Refresh Token을 httpOnly 쿠키로 설정
            if (response.getRefreshToken() != null) {
                addRefreshTokenCookie(httpServletResponse, response.getRefreshToken());
            }

            log.info("프로필 완성 성공 - userId={}, nickname={}", userId, request.getNickname());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("프로필 완성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(e.getMessage(), "INVALID_REQUEST"));

        } catch (IllegalStateException e) {
            log.warn("프로필 완성 실패 - 상태 오류: {}", e.getMessage());
            return ResponseEntity.status(409)
                    .body(ErrorResponse.of(e.getMessage(), "CONFLICT"));

        } catch (Exception e) {
            log.error("프로필 완성 실패 - 서버 오류: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

    /**
     * 닉네임 중복 체크 API
     *
     * @param nickname 확인할 닉네임
     * @return 사용 가능 여부
     */
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(@RequestParam String nickname) {
        boolean available = userService.isNicknameAvailable(nickname);

        Map<String, Boolean> response = new HashMap<>();
        response.put("available", available);

        log.info("닉네임 중복 체크 - nickname={}, available={}", nickname, available);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh Token으로 Access Token 재발급 API
     *
     * @return 새로운 Access Token
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {
        try {
            // 쿠키에서 Refresh Token 가져오기
            String refreshToken = getRefreshTokenFromCookie(request);

            if (refreshToken == null) {
                log.warn("Refresh Token이 쿠키에 없음");
                return ResponseEntity.status(401)
                        .body(ErrorResponse.of("Refresh Token이 없습니다.", "REFRESH_TOKEN_NOT_FOUND"));
            }

            // Refresh Token 유효성 검증
            if (!jwtUtil.validateToken(refreshToken)) {
                log.warn("유효하지 않은 Refresh Token");
                return ResponseEntity.status(401)
                        .body(ErrorResponse.of("유효하지 않은 Refresh Token입니다.", "INVALID_REFRESH_TOKEN"));
            }

            // Refresh Token에서 사용자 ID 추출
            Long userId = jwtUtil.getUserIdFromToken(refreshToken);

            // Redis에 저장된 Refresh Token과 비교
            if (!refreshTokenService.validateRefreshToken(userId, refreshToken)) {
                log.warn("Redis에 저장된 Refresh Token과 일치하지 않음 - userId: {}", userId);
                return ResponseEntity.status(401)
                        .body(ErrorResponse.of("Refresh Token이 일치하지 않습니다.", "REFRESH_TOKEN_MISMATCH"));
            }

            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // 새로운 Access Token 발급
            String newAccessToken = jwtUtil.generateAccessToken(userId, user.getRole().name());

            log.info("Access Token 재발급 성공 - userId: {}", userId);
            return ResponseEntity.ok(TokenResponse.of(newAccessToken, null, jwtUtil.getAccessTokenExpiration()));

        } catch (Exception e) {
            log.error("Access Token 재발급 실패: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

    /**
     * 로그아웃 API
     * Refresh Token 삭제 및 SecurityContext 초기화
     *
     * @return 로그아웃 결과
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 인증된 사용자인 경우 Refresh Token 삭제
            if (authentication != null && authentication.isAuthenticated()
                    && !(authentication instanceof AnonymousAuthenticationToken)) {
                try {
                    Long userId = Long.parseLong(authentication.getName());
                    refreshTokenService.deleteRefreshToken(userId);
                    log.info("Redis에서 Refresh Token 삭제 완료 - userId: {}", userId);
                } catch (Exception e) {
                    log.warn("Refresh Token 삭제 중 오류 발생: {}", e.getMessage());
                }
            }

            // Refresh Token 쿠키 삭제
            Cookie cookie = new Cookie("refreshToken", null);
            cookie.setHttpOnly(true);
            cookie.setPath("/");
            cookie.setMaxAge(0);  // 즉시 삭제
            response.addCookie(cookie);

            // SecurityContext 초기화
            SecurityContextHolder.clearContext();

            log.info("로그아웃 완료 - SecurityContext 초기화됨");
            return ResponseEntity.ok(LogoutResponse.success());
        } catch (Exception e) {
            log.error("로그아웃 실패: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(LogoutResponse.failure(e.getMessage()));
        }
    }

    /**
     * Refresh Token을 httpOnly 쿠키로 추가
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie("refreshToken", refreshToken);
        cookie.setHttpOnly(true);  // JavaScript에서 접근 불가 (XSS 방지)
        cookie.setSecure(false);   // HTTPS only (배포 시 true로 변경)
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60);  // 7일 (초 단위)
        response.addCookie(cookie);
        log.info("Refresh Token 쿠키 설정 완료");
    }

    /**
     * 쿠키에서 Refresh Token 가져오기
     */
    private String getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refreshToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}