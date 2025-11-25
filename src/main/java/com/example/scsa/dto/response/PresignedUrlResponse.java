package com.example.scsa.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Presigned URL 응답")
public class PresignedUrlResponse {

    @Schema(description = "업로드용 Presigned URL (프론트엔드에서 PUT 요청에 사용)",
            example = "https://tennis-app-profile-images.s3.ap-northeast-2.amazonaws.com/profiles/123/uuid.jpg?X-Amz-Algorithm=...")
    private String presignedUrl;

    @Schema(description = "업로드 완료 후 이미지의 최종 URL (DB에 저장할 URL)",
            example = "https://tennis-app-profile-images.s3.ap-northeast-2.amazonaws.com/profiles/123/uuid.jpg")
    private String imageUrl;

    @Schema(description = "Presigned URL 만료 시간 (초)",
            example = "300")
    private int expirationSeconds;
}