package com.example.scsa.controller;


import com.example.scsa.dto.profile.UserProfileDTO;
import com.example.scsa.dto.profile.UserProfileDeleteResponseDTO;
import com.example.scsa.dto.request.PresignedUrlRequest;
import com.example.scsa.dto.request.ProfileCompleteRequest;
import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.dto.response.PresignedUrlResponse;
import com.example.scsa.dto.response.ProfileCompleteResponse;
import com.example.scsa.exception.profile.UserDeleteNotAllowedException;
import com.example.scsa.exception.UserNotFoundException;
import com.example.scsa.repository.UserRepository;
import com.example.scsa.service.S3Service;
import com.example.scsa.service.UserService;
import com.example.scsa.service.profile.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "사용자 프로필 API", description = "사용자 프로필 조회 및 수정 관련 API")
public class UserProfileController {

    private final UserRepository userRepository;
    private final UserProfileService userProfileService;
    private final UserService userService;
    private final S3Service s3Service;

    /**
     * 프로필 완성 API
     * 카카오 OAuth2 로그인 후 추가 정보(nickname, gender, period, age) 입력
     *
     * @param request 프로필 완성 요청
     * @return 프로필 완성 응답 (새 JWT 토큰 포함)
     */
    @Operation(summary = "프로필 완성", description = "OAuth2 로그인 후 추가 정보(닉네임, 성별, 경력, 나이)를 입력하여 프로필을 완성합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "프로필 완성 성공",
            content = @Content(schema = @Schema(implementation = ProfileCompleteResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 요청",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "이미 프로필이 완성된 사용자",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @Operation(summary = "닉네임 중복 체크", description = "입력한 닉네임의 사용 가능 여부를 확인합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공 (available: true=사용가능, false=중복)")
    })
    @GetMapping("/check-nickname")
    public ResponseEntity<Map<String, Boolean>> checkNickname(
            @Parameter(description = "확인할 닉네임", required = true) @RequestParam String nickname) {
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
    @Operation(summary = "사용자 프로필 조회", description = "사용자 ID로 특정 사용자의 프로필 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = UserProfileDTO.class)))
    })
    @GetMapping("/{user_id}")
    public ResponseEntity<UserProfileDTO> getUserProfile(
            @Parameter(description = "사용자 ID", required = true) @PathVariable("user_id") Long userId) {
        UserProfileDTO result = userProfileService.getUserProfile(userId);
        return ResponseEntity.ok(result);
    }

    /**
     * 프로필 이미지 업로드용 Presigned URL 생성
     * POST /api/v1/users/me/profile-image/presigned-url
     */
    @Operation(summary = "프로필 이미지 업로드용 Presigned URL 생성",
            description = "S3에 프로필 이미지를 업로드하기 위한 Presigned URL을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Presigned URL 생성 성공",
                    content = @Content(schema = @Schema(implementation = PresignedUrlResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (지원하지 않는 파일 형식 등)",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "서버 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/me/profile-image/presigned-url")
    public ResponseEntity<?> generatePresignedUrl(@Valid @RequestBody PresignedUrlRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        try {
            Long userId = Long.parseLong(authentication.getName());
            log.info("Presigned URL 생성 요청 - userId: {}, fileName: {}, fileType: {}",
                    userId, request.getFileName(), request.getFileType());

            PresignedUrlResponse response = s3Service.generatePresignedUrl(userId, request);

            log.info("Presigned URL 생성 성공 - userId: {}, imageUrl: {}", userId, response.getImageUrl());
            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            log.error("잘못된 사용자 ID 형식: {}", authentication.getName());
            return ResponseEntity.status(400)
                    .body(ErrorResponse.of("잘못된 사용자 ID입니다.", "INVALID_USER_ID"));

        } catch (IllegalArgumentException e) {
            log.warn("Presigned URL 생성 실패 - 잘못된 요청: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of(e.getMessage(), "INVALID_REQUEST"));

        } catch (Exception e) {
            log.error("Presigned URL 생성 실패 - 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("Presigned URL 생성에 실패했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

    /**
     * 본인 프로필 정보 수정
     * PATCH /api/v1/users/me/update
     */
    @Operation(summary = "프로필 수정", description = "로그인한 사용자의 프로필 정보를 수정합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공",
            content = @Content(schema = @Schema(implementation = UserProfileDTO.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 사용자 ID",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "서버 오류",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
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
    @Operation(
            summary = "회원 탈퇴",
            description = "로그인한 사용자의 계정을 삭제합니다. 사용자가 개설한 모집 중인 매치가 있으면 탈퇴가 불가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "회원 탈퇴 성공",
                    content = @Content(schema = @Schema(implementation = UserProfileDeleteResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 사용자 ID 형식",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "인증되지 않은 사용자",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "모집 중인 매치가 있어 탈퇴 불가",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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

            UserProfileDeleteResponseDTO response = userProfileService.deleteUser(userId);   // 실제 삭제 로직

            SecurityContextHolder.clearContext();

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

        } catch (UserDeleteNotAllowedException e) {
            log.warn("회원 탈퇴 실패 - 본인이 개설한 매치가 존재함");
            return ResponseEntity.status(409)
                    .body(ErrorResponse.of(e.getMessage(), "MATCH_EXISTS"));

        }catch (Exception e) {
            log.error("회원 탈퇴 실패 - 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }
}
