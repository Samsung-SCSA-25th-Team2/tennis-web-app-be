package com.example.scsa.service.chat;

import com.example.scsa.domain.entity.ChatRoom;
import com.example.scsa.dto.chat.ChatReadResponseDTO;
import com.example.scsa.exception.ChatRoomAccessDeniedException;
import com.example.scsa.exception.ChatRoomNotFoundException;
import com.example.scsa.repository.ChatRepository;
import com.example.scsa.repository.ChatRoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class ChatReadService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRepository chatRepository;

    @Transactional
    public ChatReadResponseDTO markAllAsRead(Long chatRoomId, Long currentUserId) {

        // 1. 채팅방 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        // 2. 채팅방 참여자 여부 검증
        if (!isParticipant(chatRoom, currentUserId)) {
            throw new ChatRoomAccessDeniedException(chatRoomId, currentUserId);
        }

        // 3. 읽음 처리 (상대가 보낸 메시지 + 아직 안 읽은 것만)
        LocalDateTime now = LocalDateTime.now();
        int updatedCount = chatRepository.markMessagesAsRead(chatRoomId, currentUserId, now);

        // 4. 응답 DTO 생성
        String readAtStr = (updatedCount > 0)
                ? now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;

        return ChatReadResponseDTO.builder()
                .chatRoomId(chatRoomId)
                .updatedCount(updatedCount)
                .readAt(readAtStr)
                .build();
    }

    /**
     * 프로젝트의 ChatRoom 구조에 맞게 수정해서 사용하면 됨.
     * 예: user1 / user2, host / guest 등
     */
    private boolean isParticipant(ChatRoom chatRoom, Long userId) {
        return (chatRoom.getUser1() != null && chatRoom.getUser1().getId().equals(userId))
                || (chatRoom.getUser2() != null && chatRoom.getUser2().getId().equals(userId));
        // 혹은 host/guest 라면:
        // return chatRoom.getHost().getId().equals(userId) || chatRoom.getGuest().getId().equals(userId);
    }
}