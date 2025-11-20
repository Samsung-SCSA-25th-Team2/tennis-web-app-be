package com.example.scsa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그아웃 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutResponse {

    /**
     * 성공 여부
     */
    private boolean success;

    /**
     * 응답 메시지
     */
    private String message;

    /**
     * 로그아웃 성공 응답 생성
     */
    public static LogoutResponse success() {
        return LogoutResponse.builder()
                .success(true)
                .message("로그아웃되었습니다. 클라이언트에서 토큰을 삭제하세요.")
                .build();
    }

    /**
     * 로그아웃 실패 응답 생성
     */
    public static LogoutResponse failure(String errorMessage) {
        return LogoutResponse.builder()
                .success(false)
                .message("로그아웃 처리 중 오류가 발생했습니다: " + errorMessage)
                .build();
    }
}
