package com.example.scsa.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 에러 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {

    /**
     * 에러 메시지
     */
    private String error;

    /**
     * 에러 코드 (선택사항)
     */
    private String errorCode;

    /**
     * 에러 응답 생성
     */
    public static ErrorResponse of(String error) {
        return ErrorResponse.builder()
                .error(error)
                .build();
    }

    /**
     * 에러 코드와 함께 에러 응답 생성
     */
    public static ErrorResponse of(String error, String errorCode) {
        return ErrorResponse.builder()
                .error(error)
                .errorCode(errorCode)
                .build();
    }
}
