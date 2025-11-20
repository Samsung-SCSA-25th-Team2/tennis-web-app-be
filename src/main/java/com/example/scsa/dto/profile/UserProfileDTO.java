package com.example.scsa.dto.profile;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserProfileDTO {

    private String nickname;
    private String period;
    private String gender;
    private String age;
    private String imgUrl;
    private String name;
}
