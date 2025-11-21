package com.example.scsa.controller;

import com.example.scsa.dto.chat.ChatRoomCreateRequestDTO;
import com.example.scsa.dto.chat.ChatRoomCreateResponseDTO;
import com.example.scsa.dto.chat.ChatRoomListRequestDTO;
import com.example.scsa.dto.chat.ChatRoomListResponseDTO;
import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.exception.ChatRoomAlreadyExistsException;
import com.example.scsa.exception.ChatRoomNotFoundException;
import com.example.scsa.exception.UserDeleteNotAllowedException;
import com.example.scsa.exception.UserNotFoundException;
import com.example.scsa.service.chat.ChatRoomService;
import com.example.scsa.dto.auth.CustomOAuth2User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/rooms")
@Slf4j
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

        try {
            Long currentUserId = Long.parseLong(authentication.getName());
            log.info("채팅방 생성 요청 - guestId: {}, matchId: {}", currentUserId, request.getMatchId());

            ChatRoomCreateResponseDTO response =
                    chatRoomService.createChatRoom(currentUserId, request);
            log.info("채팅방 생성 성공 - guestId: {}, matchId: {}", currentUserId, request.getMatchId());

            // 명세서에 맞춰 200 OK 사용
            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("잘못된 사용자 ID 형식: {}", authentication.getName());
            return ResponseEntity.status(400)
                    .body(ErrorResponse.of("잘못된 사용자 ID입니다.", "INVALID_USER_ID"));
        } catch (ChatRoomAlreadyExistsException e) {
            log.warn("채팅방 생성 실패 - 본인이 개설한 채팅방이 존재함");
            return ResponseEntity.status(409)
                    .body(ErrorResponse.of(e.getMessage(), "CHAT_ROOM_ALREADY_EXISTS"));
        } catch (Exception e) {
            log.error("채팅방 조회 실패 - 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyChatRooms(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        try {
            Long currentUserId = Long.parseLong(authentication.getName());

            ChatRoomListRequestDTO request = new ChatRoomListRequestDTO(cursor, size);

            ChatRoomListResponseDTO response =
                    chatRoomService.getMyChatRooms(currentUserId, request);

            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("잘못된 사용자 ID 형식: {}", authentication.getName());
            return ResponseEntity.status(400)
                    .body(ErrorResponse.of("잘못된 사용자 ID입니다.", "INVALID_USER_ID"));
        } catch (Exception e) {
            log.error("채팅방 조회 실패 - 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }

    }
}