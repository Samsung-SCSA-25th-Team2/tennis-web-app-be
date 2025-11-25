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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "인증 API", description = "JWT 기반 인증, 로그인, 로그아웃, 토큰 관리 관련 API")
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
    @Operation(summary = "인증 상태 조회", description = "현재 사용자의 인증 상태 및 프로필 완성 여부를 확인합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = AuthStatusResponse.class)))
    })
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
    @Operation(summary = "현재 사용자 정보 조회", description = "로그인한 사용자의 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserInfoResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
     * Refresh Token으로 Access Token 재발급 API
     *
     * @return 새로운 Access Token
     */
    @Operation(summary = "Access Token 재발급", description = "쿠키에 저장된 Refresh Token을 사용하여 새로운 Access Token을 발급받습니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 재발급 성공",
            content = @Content(schema = @Schema(implementation = TokenResponse.class))),
        @ApiResponse(responseCode = "401", description = "유효하지 않은 Refresh Token",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request, HttpServletResponse response) {
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

            // 새로운 Access Token 및 Refresh Token 발급
            String newAccessToken = jwtUtil.generateAccessToken(userId, user.getRole().name());
            String newRefreshToken = jwtUtil.generateRefreshToken(userId);

            // Redis에 새로운 Refresh Token 저장
            refreshTokenService.saveRefreshToken(userId, newRefreshToken, jwtUtil.getRefreshTokenExpiration());

            // 쿠키에 새로운 Refresh Token 설정
            addRefreshTokenCookie(response, newRefreshToken);

            log.info("Access Token 및 Refresh Token 재발급 성공 - userId: {}", userId);
            return ResponseEntity.ok(TokenResponse.of(newAccessToken, newRefreshToken, jwtUtil.getAccessTokenExpiration()));

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
    @Operation(summary = "로그아웃", description = "사용자를 로그아웃하고 Refresh Token을 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공",
            content = @Content(schema = @Schema(implementation = LogoutResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = LogoutResponse.class)))
    })
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