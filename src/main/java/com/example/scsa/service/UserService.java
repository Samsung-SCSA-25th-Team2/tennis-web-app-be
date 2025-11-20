package com.example.scsa.service;

import com.example.scsa.domain.entity.User;
import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Gender;
import com.example.scsa.domain.vo.Period;
import com.example.scsa.dto.request.ProfileCompleteRequest;
import com.example.scsa.dto.response.ProfileCompleteResponse;
import com.example.scsa.repository.UserRepository;
import com.example.scsa.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 비즈니스 로직 처리 서비스
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    /**
     * 프로필 완성 처리
     * 카카오 OAuth2 로그인 후 추가 정보(nickname, gender, period, age) 입력
     *
     * @param userId 사용자 ID
     * @param request 프로필 완성 요청 DTO
     * @return 프로필 완성 응답 DTO (새 JWT 토큰 포함)
     */
    @Transactional
    public ProfileCompleteResponse completeProfile(Long userId, ProfileCompleteRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 2. 이미 프로필이 완성된 경우 예외 처리
        if (user.isProfileComplete()) {
            log.warn("이미 프로필이 완성된 사용자입니다. userId={}", userId);
            throw new IllegalStateException("이미 프로필이 완성되었습니다.");
        }

        // 3. 닉네임 중복 체크 (닉네임이 변경되는 경우에만)
        boolean isNicknameChanged = user.getNickname() == null
                || !user.getNickname().equals(request.getNickname());

        if (isNicknameChanged && userRepository.existsByNickname(request.getNickname())) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }

        // 4. 프로필 완성
        user.completeProfile(
                request.getNickname(),
                request.getGender(),
                request.getPeriod(),
                request.getAge()
        );

        User savedUser = userRepository.save(user);
        log.info("프로필 완성 성공 - userId={}, nickname={}", userId, request.getNickname());

        // 5. 새 JWT 토큰 생성 (Access Token + Refresh Token)
        String newAccessToken = jwtUtil.generateAccessToken(savedUser.getId(), savedUser.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(savedUser.getId());

        // 6. Refresh Token을 Redis에 저장
        refreshTokenService.saveRefreshToken(
                savedUser.getId(),
                newRefreshToken,
                jwtUtil.getRefreshTokenExpiration()
        );

        // 7. 응답 생성
        return ProfileCompleteResponse.from(savedUser, newAccessToken, newRefreshToken);
    }

    /**
     * 닉네임 중복 체크
     *
     * @param nickname 확인할 닉네임
     * @return 사용 가능 여부 (true: 사용 가능, false: 중복)
     */
    public boolean isNicknameAvailable(String nickname) {
        return !userRepository.existsByNickname(nickname);
    }
}