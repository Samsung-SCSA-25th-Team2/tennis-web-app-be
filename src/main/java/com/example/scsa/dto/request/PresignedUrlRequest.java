package com.example.scsa.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Presigned URL 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Presigned URL 요청")
public class PresignedUrlRequest {

    @NotBlank(message = "파일명은 필수입니다.")
    @Schema(description = "파일명 (예: profile.jpg)", example = "profile.jpg")
    private String fileName;

    @NotBlank(message = "파일 타입은 필수입니다.")
    @Pattern(regexp = "^image/(jpeg|jpg|png|gif|webp)$", message = "지원하지 않는 파일 형식입니다. (jpeg, jpg, png, gif, webp만 가능)")
    @Schema(description = "파일 MIME 타입 (예: image/jpeg)", example = "image/jpeg")
    private String fileType;
}