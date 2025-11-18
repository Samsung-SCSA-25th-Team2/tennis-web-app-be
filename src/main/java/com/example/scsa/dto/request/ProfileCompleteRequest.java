package com.example.scsa.dto.request;

import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Gender;
import com.example.scsa.domain.vo.Period;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로필 완성 요청 DTO
 * 카카오 OAuth2 로그인 후 추가 정보 입력
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileCompleteRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 1, max = 20, message = "닉네임은 1자 이상 20자 이하여야 합니다.")
    private String nickname;

    @NotNull(message = "성별은 필수입니다.")
    private Gender gender;

    @NotNull(message = "경력은 필수입니다.")
    private Period period;

    @NotNull(message = "나이대는 필수입니다.")
    private Age age;
}