package com.example.scsa.service.chat;

import com.example.scsa.domain.entity.Chat;
import com.example.scsa.domain.entity.ChatRoom;
import com.example.scsa.dto.chat.ChatHistoryRequestDTO;
import com.example.scsa.dto.chat.ChatHistoryResponseDTO;
import com.example.scsa.exception.ChatRoomAccessDeniedException;
import com.example.scsa.exception.ChatRoomNotFoundException;
import com.example.scsa.exception.InvalidCursorFormatException;
import com.example.scsa.repository.ChatRepository;
import com.example.scsa.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Transactional(readOnly = true)
    public ChatHistoryResponseDTO getChatHistory(
            Long roomId,
            ChatHistoryRequestDTO request,
            Long currentUserId
    ) {

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(roomId));

        if (!isParticipant(chatRoom, currentUserId)) {
            throw new ChatRoomAccessDeniedException(roomId, currentUserId);
        }

        int size = (request.getSize() == null ? 20 : request.getSize());

        LocalDateTime cursorTime = null;
        if (request.getCursor() != null) {
            try {
                cursorTime = LocalDateTime.parse(request.getCursor(), FORMATTER);
            } catch (Exception e) {
                throw new InvalidCursorFormatException(request.getCursor());
            }
        }

        int limit = size + 1;
        List<Chat> chats = chatRepository.findChatsByRoomWithCursor(roomId, cursorTime, limit);

        boolean hasNext = chats.size() > size;
        if (hasNext) chats = chats.subList(0, size);

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

        String nextCursor = messages.isEmpty()
                ? null
                : messages.get(messages.size() - 1).getCreatedAt();

        return ChatHistoryResponseDTO.builder()
                .messages(messages)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }


    private boolean isParticipant(ChatRoom chatRoom, Long userId) {
        // 너네 프로젝트 구조대로 바꾸면 됨 (host / guest or user1 / user2)
        return (chatRoom.getUser1().getId().equals(userId)
                || chatRoom.getUser2().getId().equals(userId));
    }
}