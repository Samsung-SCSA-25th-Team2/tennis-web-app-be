package com.example.scsa.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDTO {

    private Long userId;
    private String nickname;
    private String period;
    private String gender;
    private String age;
    private String imgUrl;
    private String name;
}
