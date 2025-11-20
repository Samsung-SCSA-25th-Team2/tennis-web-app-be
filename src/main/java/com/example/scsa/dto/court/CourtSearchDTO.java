package com.example.scsa.dto.court;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CourtSearchDTO {
    private List<CourtDTO> courts;  // 검색된 테니스장 목록 (없으면 빈 배열)

    private boolean hasNext; // 다음 페이지 존재 여부
    private Long cursor;   // 커서(마지막 courtId) - 없으면 0
    private Integer size;   // 현재 페이지 내 데이터 개수
}
