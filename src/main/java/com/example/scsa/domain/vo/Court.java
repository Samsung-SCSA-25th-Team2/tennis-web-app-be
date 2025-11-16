package com.example.scsa.domain.vo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 테니스 코트 값 객체 (Value Object)
 * Match 엔티티에 임베디드되어 코트 정보를 관리
 *
 * 설계 참고:
 * - @Embeddable: 별도 테이블이 아닌 Match 테이블에 컬럼으로 포함됨
 * - 불변성 권장: Setter 없이 생성자로만 객체 생성
 */
@Embeddable
@Getter
@NoArgsConstructor // JPA에서 프록시 객체 생성을 위해 필수
public class Court {

    // 코트 이름: 최대 50자
    @Column(nullable = false, length = 50)
    private String courtName;

    // 코트 주소: 최대 100자
    @Column(nullable = false, length = 100)
    private String location;

    // 위도: 지도 표시 및 위치 기반 검색에 활용
    @Column(nullable = false)
    private Double latitude;

    // 경도: 지도 표시 및 위치 기반 검색에 활용
    @Column(nullable = false)
    private Double longitude;

    // 코트 이미지 URL (선택사항)
    private String imgUrl;

    // 생성자: 검증 로직 포함
    public Court(String courtName, String location, Double latitude, Double longitude, String imgUrl) {
        validateCourtName(courtName);
        validateLocation(location);
        validateCoordinates(latitude, longitude);

        this.courtName = courtName;
        this.location = location;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imgUrl = imgUrl;
    }

    // 코트 이름 검증
    private void validateCourtName(String courtName) {
        if (courtName == null || courtName.trim().isEmpty()) {
            throw new IllegalArgumentException("코트 이름은 필수입니다.");
        }
        if (courtName.length() > 50) {
            throw new IllegalArgumentException("코트 이름은 50자 이하여야 합니다.");
        }
    }

    // 위치 검증
    private void validateLocation(String location) {
        if (location == null || location.trim().isEmpty()) {
            throw new IllegalArgumentException("코트 주소는 필수입니다.");
        }
        if (location.length() > 100) {
            throw new IllegalArgumentException("코트 주소는 100자 이하여야 합니다.");
        }
    }

    // 좌표 검증
    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("위도와 경도는 필수입니다.");
        }
        // 위도 범위: -90 ~ 90
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("위도는 -90 ~ 90 범위여야 합니다.");
        }
        // 경도 범위: -180 ~ 180
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("경도는 -180 ~ 180 범위여야 합니다.");
        }
    }

}
