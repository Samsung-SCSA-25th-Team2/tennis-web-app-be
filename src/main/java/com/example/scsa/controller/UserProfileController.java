package com.example.scsa.controller;


import com.example.scsa.dto.profile.UserProfileDTO;
import com.example.scsa.dto.request.ProfileCompleteRequest;
import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.dto.response.ProfileCompleteResponse;
import com.example.scsa.exception.UserNotFoundException;
import com.example.scsa.repository.UserRepository;
import com.example.scsa.service.UserService;
import com.example.scsa.service.profile.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Slf4j
public class UserProfileController {

    private final UserRepository userRepository;
    private final UserProfileService userProfileService;
    private final UserService userService;

    /**
     * 프로필 완성 API
     * 카카오 OAuth2 로그인 후 추가 정보(nickname, gender, period, age) 입력
     *
     * @param request 프로필 완성 요청
     * @return 프로필 완성 응답 (새 JWT 토큰 포함)
     */
    @PostMapping("/complete-profile")
    public ResponseEntity<?> completeProfile(@Valid @RequestBody ProfileCompleteRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        try {
            Long userId = Long.parseLong(authentication.getName());
            ProfileCompleteResponse response = userService.completeProfile(userId, request);

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
     * user_id로 특정 유저의 프로필 정보 조회
     * GET /api/v1/users/{user_id}
     */
    @GetMapping("/{user_id}")
    public ResponseEntity<UserProfileDTO> getUserProfile(
            @PathVariable("user_id") Long userId) {
        UserProfileDTO result = userProfileService.getUserProfile(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 본인 프로필 정보 수정
     * PATCH /api/v1/users/me/update
     */
    @PatchMapping("/me/update")
    public ResponseEntity<?> updateUserProfile(
            @RequestBody UserProfileDTO request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        try {
            Long userId = Long.parseLong(authentication.getName());
            log.info("프로필 수정 요청 - userId: {}", userId);

            UserProfileDTO response = userProfileService.updateUserProfile(userId, request);

            log.info("프로필 수정 성공 - userId: {}", userId);
            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            log.error("잘못된 사용자 ID 형식: {}", authentication.getName());
            return ResponseEntity.status(400)
                    .body(ErrorResponse.of("잘못된 사용자 ID입니다.", "INVALID_USER_ID"));

        } catch (UserNotFoundException e) {
            log.warn("프로필 수정 실패 - 사용자를 찾을 수 없음");
            return ResponseEntity.status(404)
                    .body(ErrorResponse.of(e.getMessage(), "USER_NOT_FOUND"));

        } catch (Exception e) {
            log.error("프로필 수정 실패 - 서버 오류: {}", e.getMessage());
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

    /**
     * 본인 계정 삭제 및 관련 데이터 제거
     * DELETE /api/v1/users/me/delete
     */
    @DeleteMapping("/me/delete")
    public ResponseEntity<?> deleteCurrentUser() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 401 처리: 인증 안 된 경우
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        try {
            Long userId = Long.parseLong(authentication.getName());
            log.info("회원 탈퇴 요청 - userId: {}", userId);

            userProfileService.deleteUser(userId);   // 실제 삭제 로직

            SecurityContextHolder.clearContext();

            Map<String, String> response = new HashMap<>();
            response.put("message", "회원탈퇴가 완료되었습니다.");

            log.info("회원 탈퇴 성공 - userId: {}", userId);
            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            log.error("잘못된 사용자 ID 형식: {}", authentication.getName());
            return ResponseEntity.status(400)
                    .body(ErrorResponse.of("잘못된 사용자 ID입니다.", "INVALID_USER_ID"));

        } catch (UserNotFoundException e) {
            log.warn("회원 탈퇴 실패 - 사용자를 찾을 수 없음");
            return ResponseEntity.status(404)
                    .body(ErrorResponse.of(e.getMessage(), "USER_NOT_FOUND"));

        } catch (Exception e) {
            log.error("회원 탈퇴 실패 - 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }
}
