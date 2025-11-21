package com.example.scsa.controller;

import com.example.scsa.dto.chat.ChatRoomCreateRequestDTO;
import com.example.scsa.dto.chat.ChatRoomCreateResponseDTO;
import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.service.chat.ChatRoomService;
import com.example.scsa.dto.auth.CustomOAuth2User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/rooms")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    /**
     *  채팅방 생성
     */
    @PostMapping
    public ResponseEntity<?> createChatRoom(
            @RequestBody ChatRoomCreateRequestDTO request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        Long currentUserId = Long.parseLong(authentication.getName());
        ChatRoomCreateResponseDTO response =
                chatRoomService.createChatRoom(currentUserId, request);

        // 명세서에 맞춰 200 OK 사용
        return ResponseEntity.ok(response);
    }
}