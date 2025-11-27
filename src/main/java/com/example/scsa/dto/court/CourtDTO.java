package com.example.scsa.dto.court;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourtDTO {
    private Long courtId;
    private String thumbnail;
    private Double latitude;
    private Double longitude;
    private String address;
    private String name;

}
