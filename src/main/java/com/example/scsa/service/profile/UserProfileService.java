package com.example.scsa.service.profile;

import com.example.scsa.domain.entity.User;
import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Gender;
import com.example.scsa.domain.vo.MatchStatus;
import com.example.scsa.domain.vo.Period;
import com.example.scsa.dto.profile.UserProfileDTO;
import com.example.scsa.dto.profile.UserProfileDeleteResponseDTO;
import com.example.scsa.exception.InvalidProfileUpdateException;
import com.example.scsa.exception.UserDeleteNotAllowedException;
import com.example.scsa.exception.UserNotFoundException;
import com.example.scsa.repository.MatchRepository;
import com.example.scsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException());

        return UserProfileDTO.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .period(user.getPeriod().toString())
                .gender(user.getGender().toString())
                .age(user.getAge().toString())
                .imgUrl(user.getImgUrl())
                .name(user.getName())
                .build();
    }

    @Transactional
    public UserProfileDTO updateUserProfile(Long userId, UserProfileDTO dto){

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

        return UserProfileDTO.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .period(user.getPeriod().toString())
                .gender(user.getGender().toString())
                .age(user.getAge().toString())
                .imgUrl(user.getImgUrl())
                .name(user.getName())
                .build();
    }

    @Transactional
    public UserProfileDeleteResponseDTO deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException());

        boolean hasRecruitingMatches =
                matchRepository.existsByHost_IdAndMatchStatus(userId, MatchStatus.RECRUITING);

        if (hasRecruitingMatches) {
            throw new UserDeleteNotAllowedException(
                    "현재 모집 중인 매치가 있어 탈퇴할 수 없습니다."
            );
        }

        matchRepository.deleteAllByHost_IdAndMatchStatus(userId, MatchStatus.COMPLETED);

        userRepository.delete(user);

        return UserProfileDeleteResponseDTO.builder()
                .userId(userId)
                .message("회원탈퇴가 완료되었습니다.")
                .build();
    }
}
