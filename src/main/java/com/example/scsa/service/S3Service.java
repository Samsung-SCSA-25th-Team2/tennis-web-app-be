package com.example.scsa.service;

import com.example.scsa.config.S3Config;
import com.example.scsa.dto.request.PresignedUrlRequest;
import com.example.scsa.dto.response.PresignedUrlResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

/**
 * S3 파일 업로드 서비스
 * Presigned URL 방식으로 클라이언트가 직접 S3에 업로드하도록 지원
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Presigner s3Presigner;
    private final S3Config s3Config;

    /**
     * 프로필 이미지 업로드용 Presigned URL 생성
     *
     * @param userId 사용자 ID
     * @param request 파일 정보 (fileName, fileType)
     * @return Presigned URL 및 최종 이미지 URL
     */
    public PresignedUrlResponse generatePresignedUrl(Long userId, PresignedUrlRequest request) {
        try {
            // 1. 고유한 파일명 생성 (UUID + 확장자)
            String fileExtension = extractFileExtension(request.getFileName());
            String uniqueFileName = UUID.randomUUID().toString() + fileExtension;

            // 2. S3 객체 키 생성 (profiles/{userId}/{uniqueFileName})
            String objectKey = String.format("profiles/%d/%s", userId, uniqueFileName);

            log.info("Generating presigned URL - userId: {}, objectKey: {}, fileType: {}",
                    userId, objectKey, request.getFileType());

            // 3. PutObjectRequest 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(objectKey)
                    .contentType(request.getFileType())
                    .build();

            // 4. Presigned URL 생성 (만료 시간: 설정값, 기본 5분)
            PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(s3Config.getPresignedUrlExpiration()))
                    .putObjectRequest(putObjectRequest)
                    .build();

            PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

            // 5. 최종 이미지 URL 생성 (Presigned URL에서 쿼리 파라미터 제거)
            String imageUrl = String.format("https://%s.s3.%s.amazonaws.com/%s",
                    s3Config.getBucketName(),
                    s3Config.getRegion(),
                    objectKey);

            log.info("Presigned URL generated successfully - imageUrl: {}", imageUrl);

            return PresignedUrlResponse.builder()
                    .presignedUrl(presignedRequest.url().toString())
                    .imageUrl(imageUrl)
                    .expirationSeconds(s3Config.getPresignedUrlExpiration())
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate presigned URL - userId: {}, error: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Presigned URL 생성에 실패했습니다.", e);
        }
    }

    /**
     * 파일명에서 확장자 추출
     * 예: "profile.jpg" -> ".jpg"
     */
    private String extractFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return ""; // 확장자 없음
        }
        return fileName.substring(lastDotIndex);
    }

    /**
     * S3에서 이미지 삭제 (선택 사항)
     * 프로필 이미지 변경 시 기존 이미지 삭제용
     *
     * @param imageUrl 삭제할 이미지의 전체 URL
     */
    public void deleteImage(String imageUrl) {
        try {
            // URL에서 객체 키 추출
            String objectKey = extractObjectKeyFromUrl(imageUrl);

            if (objectKey == null) {
                log.warn("Invalid S3 URL format - cannot extract object key: {}", imageUrl);
                return;
            }

            // S3에서 삭제 (실제 삭제 로직은 필요 시 구현)
            log.info("Image deletion requested - objectKey: {}", objectKey);
            // s3Client.deleteObject(DeleteObjectRequest.builder()
            //         .bucket(s3Config.getBucketName())
            //         .key(objectKey)
            //         .build());

        } catch (Exception e) {
            log.error("Failed to delete image - url: {}, error: {}", imageUrl, e.getMessage(), e);
            // 삭제 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }

    /**
     * S3 URL에서 객체 키 추출
     * 예: "https://bucket.s3.region.amazonaws.com/profiles/123/uuid.jpg" -> "profiles/123/uuid.jpg"
     */
    private String extractObjectKeyFromUrl(String imageUrl) {
        try {
            String prefix = String.format("https://%s.s3.%s.amazonaws.com/",
                    s3Config.getBucketName(),
                    s3Config.getRegion());

            if (imageUrl.startsWith(prefix)) {
                return imageUrl.substring(prefix.length());
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to extract object key from URL: {}", imageUrl, e);
            return null;
        }
    }
}