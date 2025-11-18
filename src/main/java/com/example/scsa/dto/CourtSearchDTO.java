package com.example.scsa.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CourtSearchDTO {
    private List<CourtDTO> courts;

    private boolean hasNext; // 다음 페이지 존재 여부
    private int page;   // 현재 페이지 번호
    private int size;   // 페이지 크기
}
