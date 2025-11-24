package com.example.scsa.dto.match;


import lombok.*;

/**
 * 매치 목록 조회 검색 조건 DTO
 * GET /matches 쿼리 파라미터를 객체로 매핑
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchListRequestDTO {

    /**
     * 정렬 기준
     * - createdAt (기본값)
     * - latest
     * - distance
     * - recommend
     */
    @Builder.Default
    private String sort = "createdAt";

    /**
     * 조회 시작 날짜 (YYYY-MM-DD)
     * - null인 경우: 서버에서 오늘 날짜로 기본값 처리
     */
    private String startDate;

    /**
     * 조회 종료 날짜 (YYYY-MM-DD)
     * - null인 경우: 서버에서 9999-12-31로 기본값 처리
     */
    private String endDate;

    /**
     * 조회 시작 시간 (0~23)
     * - null인 경우: 0
     */
    @Builder.Default
    private Integer startTime = 0;

    /**
     * 조회 종료 시간 (1~24)
     * - null인 경우: 24
     */
    @Builder.Default
    private Integer endTime = 24;

    /**
     * 게임 유형
     * - 예: SINGLES, MEN_DOUBLES, WOMEN_DOUBLES, MIXED_DOUBLES
     * - null이면 필터링 하지 않음
     */
    private String gameType;

    /**
     * 사용자 위도
     * - sort=distance 일 때 필수
     * - 그 외에는 null 허용 (서버에서 기본값 사용 가능)
     */
    private Double latitude;

    /**
     * 사용자 경도
     * - sort=distance 일 때 필수
     * - 그 외에는 null 허용 (서버에서 기본값 사용 가능)
     */
    private Double longitude;

    /**
     * 검색 반경(km)
     * - null이면 기본값 25km
     */
    @Builder.Default
    private Integer radius = 25;

    /**
     * 매치 상태 필터
     * - null: RECRUITING 만
     * - RECRUITING
     * - COMPLETED
     * - RECRUITING,COMPLETED
     */
    @Builder.Default
    private String status = "RECRUITING";

    /**
     * 페이지 크기
     * - 기본값 10
     */
    @Builder.Default
    private Integer size = 10;

    /**
     * 커서 (Base64 인코딩된 문자열)
     * - 첫 페이지 조회 시 null
     */
    private String cursor;
}
