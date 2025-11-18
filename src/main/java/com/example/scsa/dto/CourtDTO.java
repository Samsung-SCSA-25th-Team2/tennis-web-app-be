package com.example.scsa.dto;

import lombok.Builder;
import lombok.Getter;


@Getter
@Builder
public class CourtDTO {
    private Long courtId;
    private String thumbnail;
    private Double latitude;
    private Double longitude;
    private String address;
    private String name;

}
