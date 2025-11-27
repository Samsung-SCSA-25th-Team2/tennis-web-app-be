package com.example.scsa.dto.profile;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDeleteResponseDTO {
    private Long userId;
    private String message;
}
