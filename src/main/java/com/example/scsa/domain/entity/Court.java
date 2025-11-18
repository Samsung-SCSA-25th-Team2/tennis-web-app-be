package com.example.scsa.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 테니스 코트 엔티티
 * 테니스 코트 정보를 관리하는 독립적인 엔티티
 *
 * 설계 참고:
 * - @Entity: 별도 테이블로 관리됨
 * - Match 엔티티와 다대일 관계로 연결
 */
@Entity
@Table(name = "court")
@Getter
@NoArgsConstructor
public class Court {

    // 기본키: 자동 증가 방식
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "court_id")
    private Long id;

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
