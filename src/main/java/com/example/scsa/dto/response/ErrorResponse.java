package com.example.scsa.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 에러 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "에러 응답")
public class ErrorResponse {

    /**
     * 에러 메시지
     */
    @Schema(description = "에러 메시지", example = "잘못된 요청입니다.")
    private String error;

    /**
     * 에러 코드
     */
    @Schema(description = "에러 코드", example = "COMMON-001")
    private String errorCode;

    /**
     * 발생 시각
     */
    @Schema(description = "에러 발생 시각", example = "2025-01-19T12:00:00")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * 필드별 에러 정보 (유효성 검증 실패 시)
     */
    @Schema(description = "필드별 에러 정보")
    private List<FieldError> fieldErrors;

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

    /**
     * 필드 에러 정보
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "필드 에러 정보")
    public static class FieldError {
        @Schema(description = "필드명", example = "email")
        private String field;

        @Schema(description = "입력값", example = "invalid-email")
        private String value;

        @Schema(description = "에러 메시지", example = "올바른 이메일 형식이 아닙니다.")
        private String reason;
    }
}

