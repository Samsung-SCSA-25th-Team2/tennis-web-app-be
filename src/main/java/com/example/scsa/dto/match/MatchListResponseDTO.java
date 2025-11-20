package com.example.scsa.dto.match;

import lombok.Builder;
import lombok.Data;

import java.util.List;


@Data
@Builder
public class MatchListResponseDTO {

    private List<MatchSearchDTO> matches;

    private Long size;
    private Boolean hasNext;
    private Long cursor;
}
