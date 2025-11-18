package com.example.scsa.controller;


import com.example.scsa.dto.profile.UserProfileDTO;
import com.example.scsa.service.profile.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * user_id로 특정 유저의 프로필 정보 조회
     * GET /api/v1/users/{user_id}
     */
    @GetMapping("/{user_id}")
    public ResponseEntity<UserProfileDTO> getUserProfile(
            @PathVariable("user_id") Long userId) {
        UserProfileDTO result = userProfileService.getUserProfile(userId);
        return ResponseEntity.ok(result);
    }

}
