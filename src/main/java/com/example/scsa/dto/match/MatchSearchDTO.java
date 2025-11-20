package com.example.scsa.dto.match;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MatchSearchDTO {

    @NotNull
    private Long matchId;

    @NotNull
    private Long hostId;

    @NotNull
    private String startDateTime;

    @NotNull
    private String endDateTime;

    @NotNull
    private String gameType;

    @NotNull
    private Long courtId;

    @NotNull
    private List<String> period;

    @NotNull
    private Long playerCountMen;

    @NotNull
    private Long playerCountWomen;

    @NotNull
    private List<String> ageRange;

    @NotNull
    private Long fee;

    private String description;

    @NotNull
    private String status;

    @NotNull
    private String createdAt;

    private String updatedAt;
}
