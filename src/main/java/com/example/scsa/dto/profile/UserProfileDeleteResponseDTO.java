package com.example.scsa.dto.profile;


import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileDeleteResponseDTO {
    private Long userId;
    private String message;
}
