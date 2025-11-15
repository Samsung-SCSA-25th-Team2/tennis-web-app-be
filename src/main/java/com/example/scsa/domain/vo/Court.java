package com.example.scsa.domain.vo;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor // 불변 객체 생성을 위한 전체 필드 생성자
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

}
