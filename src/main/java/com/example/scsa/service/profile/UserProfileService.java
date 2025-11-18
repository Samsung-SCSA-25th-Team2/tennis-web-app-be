package com.example.scsa.service.profile;

import com.example.scsa.domain.entity.User;
import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Gender;
import com.example.scsa.domain.vo.Period;
import com.example.scsa.dto.profile.UserProfileDTO;
import com.example.scsa.dto.profile.UserProfileUpdateRequestDTO;
import com.example.scsa.dto.profile.UserProfileUpdateResponseDTO;
import com.example.scsa.exception.InvalidProfileUpdateException;
import com.example.scsa.exception.UserNotFoundException;
import com.example.scsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException());

        return UserProfileDTO.builder()
                .nickname(user.getNickname())
                .period(user.getPeriod().toString())
                .gender(user.getGender().toString())
                .age(user.getAge().toString())
                .imgUrl(user.getImgUrl())
                .name(user.getName())
                .build();
    }

    @Transactional
    public UserProfileUpdateResponseDTO updateUserProfile(Long userId, UserProfileUpdateRequestDTO dto){

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());

        if (dto.getNickname() != null){
            String nickname = dto.getNickname().trim();
            if (nickname.isEmpty()) {
                throw new IllegalArgumentException("닉네임은 필수입니다.");
            }
            if (nickname.length() > 20) {
                throw new IllegalArgumentException("닉네임은 20자 이하여야 합니다.");
            }
            if (userRepository.existsByNicknameAndIdNot(nickname, userId)){
                throw new InvalidProfileUpdateException("이미 사용중인 닉네임입니다.");
            }
            user.updateProfile(nickname, dto.getImgUrl());
        }

        if (dto.getPeriod() != null){
            user.updatePeriod(Period.valueOf(dto.getPeriod()));
        }

        if (dto.getGender() != null){
            user.updateGender(Gender.valueOf(dto.getGender()));
        }

        if (dto.getAge() != null){
            user.updateAge(Age.valueOf(dto.getAge()));
        }

        return UserProfileUpdateResponseDTO.builder()
                .nickname(user.getNickname())
                .period(user.getPeriod().toString())
                .gender(user.getGender().toString())
                .age(user.getAge().toString())
                .imgUrl(user.getImgUrl())
                .name(user.getName())
                .build();
    }
}
