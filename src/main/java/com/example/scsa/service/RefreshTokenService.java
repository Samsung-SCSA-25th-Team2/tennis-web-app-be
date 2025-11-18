package com.example.scsa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Refresh Token 관리 서비스
 * Redis를 사용하여 Refresh Token을 저장/조회/삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String REFRESH_TOKEN_PREFIX = "refresh_token:";

    /**
     * Refresh Token 저장
     *
     * @param userId       사용자 ID
     * @param refreshToken Refresh Token
     * @param expiration   만료 시간 (밀리초)
     */
    public void saveRefreshToken(Long userId, String refreshToken, long expiration) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        redisTemplate.opsForValue().set(key, refreshToken, expiration, TimeUnit.MILLISECONDS);
        log.info("Refresh Token 저장 완료 - userId: {}, expiration: {}ms", userId, expiration);
    }

    /**
     * Refresh Token 조회
     *
     * @param userId 사용자 ID
     * @return Refresh Token (없으면 null)
     */
    public String getRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        String token = redisTemplate.opsForValue().get(key);
        log.info("Refresh Token 조회 - userId: {}, exists: {}", userId, token != null);
        return token;
    }

    /**
     * Refresh Token 삭제
     *
     * @param userId 사용자 ID
     */
    public void deleteRefreshToken(Long userId) {
        String key = REFRESH_TOKEN_PREFIX + userId;
        Boolean deleted = redisTemplate.delete(key);
        log.info("Refresh Token 삭제 - userId: {}, deleted: {}", userId, deleted);
    }

    /**
     * Refresh Token 유효성 검증
     *
     * @param userId       사용자 ID
     * @param refreshToken Refresh Token
     * @return 유효 여부
     */
    public boolean validateRefreshToken(Long userId, String refreshToken) {
        String storedToken = getRefreshToken(userId);
        boolean isValid = storedToken != null && storedToken.equals(refreshToken);
        log.info("Refresh Token 검증 - userId: {}, valid: {}", userId, isValid);
        return isValid;
    }
}