package com.example.scsa.controller;

import com.example.scsa.domain.entity.User;
import com.example.scsa.dto.request.ProfileCompleteRequest;
import com.example.scsa.dto.response.AuthStatusResponse;
import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.dto.response.LogoutResponse;
import com.example.scsa.dto.response.ProfileCompleteResponse;
import com.example.scsa.dto.response.UserInfoResponse;
import com.example.scsa.repository.UserRepository;
import com.example.scsa.service.UserService;
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
     * 로그아웃 API
     * SecurityContext를 초기화 (클라이언트에서 localStorage의 토큰 삭제 필요)
     * - 당장 api 호출은 필요없음
     * - 블랙리스트를 redis에 올릴 때만, 로그아웃 API 필요
     *
     * @return 로그아웃 결과
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout() {
        try {
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
}