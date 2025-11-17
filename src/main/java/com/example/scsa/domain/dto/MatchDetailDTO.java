package com.example.scsa.domain.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MatchDetailDTO {
    private Long matchId;
    private Long hostId;
    private String startDate;
    private String endDate;
    private String gameType;
    private Long courtId;
    private String level;
    private Integer playerCountMen;
    private Integer playerCountWomen;
    private String ageRange;
    private Long fee;
    private String description;
    private String status;
    private String createdAt;
    private String updatedAt;
}
