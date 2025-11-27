package com.example.scsa.dto.match;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDTO {

    @NotNull
    private LocalDateTime startDateTime;

    @NotNull
    private LocalDateTime endDateTime;

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
}
