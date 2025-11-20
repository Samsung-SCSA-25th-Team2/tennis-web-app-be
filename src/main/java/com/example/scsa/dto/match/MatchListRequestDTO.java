package com.example.scsa.dto.match;

import lombok.Data;

@Data
public class MatchListRequestDTO {

    private String sort;
    private String date;
    private String startTime;
    private String endTime;
    private String gameType;
    private Double latitude;
    private Double longitude;
    private Long radius;
    private String status;
    private Integer size;
    private Long cursor;
}
