package com.example.scsa.service.profile;

import com.example.scsa.domain.entity.User;
import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Gender;
import com.example.scsa.domain.vo.MatchStatus;
import com.example.scsa.domain.vo.Period;
import com.example.scsa.dto.profile.UserProfileDTO;
import com.example.scsa.dto.profile.UserProfileDeleteResponseDTO;
import com.example.scsa.exception.profile.InvalidProfileUpdateException;
import com.example.scsa.exception.profile.UserDeleteNotAllowedException;
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

    /**
     * 회원 프로필 조회
     *
     * @param userId 조회할 사용자 ID
     * @return UserProfileDTO (닉네임, 기간, 성별, 나이, 프로필 이미지 등)
     */
    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return UserProfileDTO.builder()
                .userId(user.getUserId())
                .nickname(user.getNickname())
                .period(user.getPeriod().toString())
                .gender(user.getGender().toString())
                .age(user.getAge().toString())
                .imgUrl(user.getImgUrl())
                .build();
    }

    /**
     * 회원 프로필 수정
     *
     * - nickname, period, gender, age, imgUrl 등을 부분 업데이트
     * - null이 아닌 필드만 수정
     * - 닉네임 중복/형식 검증 수행
     */
    @Transactional
    public UserProfileDTO updateUserProfile(Long userId, UserProfileDTO dto){

        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 닉네임 변경 요청이 있는 경우에만 검증 + 업데이트
        if (dto.getNickname() != null){
            String nickname = dto.getNickname().trim();

            // 기본 형식 검증
            if (nickname.isEmpty()) {
                throw new IllegalArgumentException("닉네임은 필수입니다.");
            }
            if (nickname.length() > 20) {
                throw new IllegalArgumentException("닉네임은 20자 이하여야 합니다.");
            }

            // 다른 유저가 이미 사용 중인지 검사
            if (userRepository.existsByNicknameAndIdNot(nickname, userId)){
                throw new InvalidProfileUpdateException("이미 사용중인 닉네임입니다.");
            }

            // 닉네임 + 프로필 이미지 동시 업데이트
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

    /**
     * 회원 탈퇴
     *
     * 비즈니스 규칙:
     *  1) 내가 Host로 "모집 중(RECRUITING)"인 매치가 하나라도 있으면 탈퇴 불가
     *  2) 내가 게스트로 참여한 MatchGuest 기록은 먼저 제거
     *  3) 내가 참여한 모든 채팅방(ChatRoom) 삭제 → Chat은 cascade로 같이 삭제
     *  4) 내가 Host인 COMPLETED 매치는 모두 삭제
     *  5) 마지막으로 User 엔티티 삭제
     *
     * 이렇게 단계적으로 삭제하는 이유:
     *  - 탈퇴한 유저가 매치나 채팅방에 남지 않도록 참조 관계를 정리
     *  - DB 제약조건/외래키, 도메인 일관성을 깨지 않기 위함
     */
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

        // 2. 내가 게스트로 참가 중인 매치 기록 삭제
        //    - 탈퇴 후에도 match_guest 테이블에 유저가 남지 않도록 정리
        matchGuestRepository.deleteByUser_Id(userId);

        // 3. 내가 참여한 모든 채팅방 삭제 (Host/Guest 상관없이)
        //    - ChatRoom 삭제 시 Chat은 cascade로 같이 삭제되도록 설계
        chatRoomRepository.deleteByUser1_IdOrUser2_Id(userId, userId);

        // 4. 내가 Host인 COMPLETED 매치 삭제
        //    - COMPLETED 상태만 삭제 (RECRUITING은 1번 체크에서 이미 존재하지 않음)
        //    - 매치 삭제 시 관련 MatchGuest, 연관 VO 매핑 테이블도 cascade로 삭제
        matchRepository.deleteAllByHost_IdAndMatchStatus(userId, MatchStatus.COMPLETED);

        // 5. 마지막으로 User 삭제
        userRepository.delete(user);

        return UserProfileDeleteResponseDTO.builder()
                .userId(userId)
                .message("회원탈퇴가 완료되었습니다.")
                .build();
    }
}