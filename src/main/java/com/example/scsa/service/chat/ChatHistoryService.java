package com.example.scsa.service.chat;

import com.example.scsa.domain.entity.Chat;
import com.example.scsa.domain.entity.ChatRoom;
import com.example.scsa.dto.chat.ChatHistoryRequestDTO;
import com.example.scsa.dto.chat.ChatHistoryResponseDTO;
import com.example.scsa.exception.chat.ChatRoomAccessDeniedException;
import com.example.scsa.exception.chat.ChatRoomNotFoundException;
import com.example.scsa.exception.chat.InvalidCursorFormatException;
import com.example.scsa.repository.ChatRepository;
import com.example.scsa.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;

    // Z를 붙여서 응답 (프론트엔드 규약: 2025-11-17T19:00:00Z)
    private static final DateTimeFormatter FORMATTER = new DateTimeFormatterBuilder()
            .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
            .appendLiteral('Z')
            .toFormatter();

    /**
     * 특정 채팅방의 과거 메시지 조회 (커서 기반 페이징)
     *
     * @param roomId          조회할 채팅방 ID
     * @param request         커서(cursor)와 size(조회할 개수)를 포함한 요청 정보
     * @param currentUserId   현재 로그인한 사용자 ID
     *
     * 1) 채팅방 존재 여부 확인
     * 2) 현재 유저가 채팅방 참가자인지 권한 검증
     * 3) 커서(cursor) 파싱 (유효성 검사)
     * 4) size + 1 만큼 조회하여 hasNext 판단
     * 5) 메시지 목록 DTO 변환
     * 6) nextCursor 계산하여 반환
     */
    @Transactional(readOnly = true)
    public ChatHistoryResponseDTO getChatHistory(
            Long roomId,
            ChatHistoryRequestDTO request,
            Long currentUserId
    ) {

        // 1. 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(roomId));

        // 2. 해당 채팅방의 참여자인지 확인 (본인/상대방)
        if (!isParticipant(chatRoom, currentUserId)) {
            throw new ChatRoomAccessDeniedException(roomId, currentUserId);
        }

        int size = (request.getSize() == null ? 20 : request.getSize());

        // 3. 커서값(마지막 메시지 생성시간)을 LocalDateTime으로 파싱
        LocalDateTime cursorTime = null;
        if (request.getCursor() != null) {
            try {
                cursorTime = LocalDateTime.parse(request.getCursor(), FORMATTER);
            } catch (Exception e) {
                throw new InvalidCursorFormatException(request.getCursor());
            }
        }

        // 4. size + 1만큼 가져와 다음 페이지 유무(hasNext)를 판단
        //    (커서 시간보다 이전의 메시지들 조회)
        int limit = size + 1;
        List<Chat> chats = chatRepository.findChatsByRoomWithCursor(roomId, cursorTime, limit);

        // 5. 전체 조회된 개수가 size보다 많으면 다음 페이지가 존재하는 것
        boolean hasNext = chats.size() > size;
        if (hasNext)
            chats = chats.subList(0, size);

        // 6. Chat → MessageItem DTO 변환
        List<ChatHistoryResponseDTO.MessageItem> messages = chats.stream()
                .map(c -> ChatHistoryResponseDTO.MessageItem.builder()
                        .chatId(c.getId())
                        .chatRoomId(roomId)
                        .senderId(c.getSender().getId())
                        .senderNickname(c.getSender().getNickname())
                        .senderImgUrl(c.getSender().getImgUrl())
                        .message(c.getMessage())
                        .createdAt(c.getCreatedAt().format(FORMATTER))
                        .isRead(c.getIsRead())
                        .readAt(c.getReadAt() == null ? null : c.getReadAt().format(FORMATTER))
                        .isMine(c.getSender().getId().equals(currentUserId))
                        .build())
                .collect(Collectors.toList());

        // 7. nextCursor = 마지막 메시지의 createdAt(시간)
        String nextCursor = messages.isEmpty()
                ? null
                : messages.get(messages.size() - 1).getCreatedAt();

        // 8. 응답 DTO 반환
        return ChatHistoryResponseDTO.builder()
                .messages(messages)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }

    /**
     * 채팅방의 두 유저(user1 / user2) 중 현재 사용자 ID가 포함되어 있는지 확인
     */
    private boolean isParticipant(ChatRoom chatRoom, Long userId) {
        return (chatRoom.getUser1().getId().equals(userId)
                || chatRoom.getUser2().getId().equals(userId));
    }
}