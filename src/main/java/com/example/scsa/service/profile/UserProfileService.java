package com.example.scsa.service.profile;

import com.example.scsa.domain.entity.User;
import com.example.scsa.dto.profile.UserProfileDTO;
import com.example.scsa.exception.UserNotFoundException;
import com.example.scsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(Long userId){
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        return UserProfileDTO.builder()
                .nickname(user.getNickname())
                .period(user.getPeriod().toString())
                .gender(user.getGender().toString())
                .age(user.getAge().toString())
                .imgUrl(user.getImgUrl())
                .name(user.getName())
                .build();
    }
}
