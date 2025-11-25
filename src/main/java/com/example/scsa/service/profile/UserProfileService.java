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
import com.example.scsa.repository.ChatRoomRepository;
import com.example.scsa.repository.MatchGuestRepository;
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
    private final MatchGuestRepository matchGuestRepository;
    private final ChatRoomRepository chatRoomRepository;

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
                .orElseThrow(UserNotFoundException::new);

        // 1. Host로서 모집중인 매치가 있으면 탈퇴 불가
        boolean hasRecruitingMatches =
                matchRepository.existsByHost_IdAndMatchStatus(userId, MatchStatus.RECRUITING);

        if (hasRecruitingMatches) {
            throw new UserDeleteNotAllowedException(
                    "현재 모집 중인 매치가 있어 탈퇴할 수 없습니다."
            );
        }

        // 2. 내가 게스트로 참가 중인 MatchGuest 기록 먼저 삭제
        //    (탈퇴한 유저가 매치 참가자 목록에 남지 않도록)
        matchGuestRepository.deleteByUser_Id(userId);

        // 3. 내가 참여한 모든 채팅방 삭제 (Host/Guest 상관없이)
        //    - ChatRoom 삭제 → Chat은 cascade로 자동 삭제
        chatRoomRepository.deleteByUser1_IdOrUser2_Id(userId, userId);

        // 4. 내가 Host인 COMPLETED 매치 삭제
        //    - Match 삭제 → MatchGuest, match_age/gender/period는 cascade로 자동 삭제
        matchRepository.deleteAllByHost_IdAndMatchStatus(userId, MatchStatus.COMPLETED);

        // 5. 마지막으로 User 삭제
        userRepository.delete(user);

        return UserProfileDeleteResponseDTO.builder()
                .userId(userId)
                .message("회원탈퇴가 완료되었습니다.")
                .build();
    }
}
