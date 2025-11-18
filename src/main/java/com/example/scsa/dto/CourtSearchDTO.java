package com.example.scsa.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CourtSearchDTO {
    private List<CourtDTO> courts;
}
