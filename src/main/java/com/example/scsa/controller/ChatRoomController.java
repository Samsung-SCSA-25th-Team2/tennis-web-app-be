package com.example.scsa.controller;

import com.example.scsa.dto.chat.*;
import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.exception.*;
import com.example.scsa.service.chat.ChatReadService;
import com.example.scsa.service.chat.ChatRoomService;
import com.example.scsa.dto.auth.CustomOAuth2User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "채팅방", description = "1:1 채팅방 관리 API (STOMP WebSocket 기반)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/chat/rooms")
@Slf4j
public class ChatRoomController {

    private final ChatRoomService chatRoomService;
    private final ChatReadService chatReadService;

    @Operation(
        summary = "채팅방 생성",
        description = "매치에서 호스트와 게스트 간의 1:1 채팅방을 생성합니다. " +
                     "같은 매치에서 동일한 두 사용자 간에는 하나의 채팅방만 생성 가능합니다. " +
                     "생성된 채팅방 ID를 사용하여 WebSocket으로 실시간 채팅이 가능합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "채팅방 생성 성공",
            content = @Content(schema = @Schema(implementation = ChatRoomCreateResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "이미 존재하는 채팅방",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @PostMapping
    public ResponseEntity<?> createChatRoom(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "채팅방 생성 요청 정보 (매치 ID와 게스트 ID 필요)",
                required = true
            )
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

    @Operation(
        summary = "내 채팅방 목록 조회",
        description = "현재 로그인한 사용자의 채팅방 목록을 커서 기반 페이징으로 조회합니다. " +
                     "최근 메시지 시간 순으로 정렬되며, 각 채팅방의 안읽은 메시지 수와 마지막 메시지 미리보기를 포함합니다."
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "채팅방 목록 조회 성공",
            content = @Content(schema = @Schema(implementation = ChatRoomListResponseDTO.class))
        ),
        @ApiResponse(
            responseCode = "401",
            description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    @GetMapping("/my")
    public ResponseEntity<?> getMyChatRooms(
            @Parameter(description = "페이징 커서 (마지막 메시지 시간, ISO-8601 형식)", example = "2025-11-24T12:34:56")
            @RequestParam(required = false) String cursor,
            @Parameter(description = "한 번에 조회할 채팅방 수", example = "20")
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

    @PatchMapping("/{chatRoomId}/read")
    public ResponseEntity<?> markMessagesAsRead(
            @PathVariable Long chatRoomId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        try {
            Long currentUserId = Long.parseLong(authentication.getName());
            log.info("메세지 읽음 처리 요청 - userId: {}, chatRoomId: {}", currentUserId, chatRoomId);

            ChatReadResponseDTO response =  chatReadService.markAllAsRead(chatRoomId, currentUserId);
            log.info("메세지 읽음 처리 성공 - userId: {}, chatRoomId: {}", currentUserId, chatRoomId);

            return ResponseEntity.ok(response);
        } catch (NumberFormatException e) {
            log.error("잘못된 사용자 ID 형식: {}", authentication.getName());
            return ResponseEntity.status(400)
                    .body(ErrorResponse.of("잘못된 사용자 ID입니다.", "INVALID_USER_ID"));
        } catch (ChatRoomAccessDeniedException e) {
            log.error("채팅방 접근 권한 없음");
            return ResponseEntity.status(403)
                    .body(ErrorResponse.of("채팅방 접근 권한이 없습니다", "CHAT_ROOM_ACCESS_DENIED"));
        } catch (ChatRoomNotFoundException e) {
            log.error("채팅방이 존재하지 않음");
            return ResponseEntity.status(404)
                    .body(ErrorResponse.of("채팅방이 존재하지 않습니다", "CHAT_ROOM_NOT_FOUND"));
        } catch(Exception e) {
            log.error("채팅방 조회 실패 - 서버 오류: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }
}