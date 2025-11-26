package com.example.scsa.service.chat;

import com.example.scsa.domain.entity.ChatRoom;
import com.example.scsa.dto.chat.ChatReadResponseDTO;
import com.example.scsa.exception.chat.ChatRoomAccessDeniedException;
import com.example.scsa.exception.chat.ChatRoomNotFoundException;
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

    /**
     * 특정 채팅방에서 "상대가 보낸 메시지 중 아직 읽지 않은 메시지"를 모두 읽음 처리한다.
     *
     * 처리 절차:
     *  1) 채팅방 존재 여부 확인
     *  2) 현재 사용자(currentUserId)가 해당 채팅방의 참여자인지 검증
     *  3) 읽지 않은 메시지를 일괄 업데이트 (상대방이 보낸 메세지 + isRead = false)
     *  4) 실제 읽음 처리된 메시지 수 및 읽은 시각(readAt)을 응답 DTO로 반환
     *
     * @param chatRoomId     읽음 처리할 채팅방 ID
     * @param currentUserId  현재 로그인한 사용자 ID
     */
    @Transactional
    public ChatReadResponseDTO markAllAsRead(Long chatRoomId, Long currentUserId) {

        // 1. 채팅방 조회 (없으면 예외 발생)
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new ChatRoomNotFoundException(chatRoomId));

        // 2. 접근 권한 체크: user1 또는 user2 중 하나인지 확인
        if (!isParticipant(chatRoom, currentUserId)) {
            throw new ChatRoomAccessDeniedException(chatRoomId, currentUserId);
        }

        // 3. 읽음 처리 로직 실행
        //    markMessagesAsRead(chatRoomId, userId, now):
        //    → userId가 아닌 "상대방이 보낸" 메시지 중 isRead=false인 것들만 업데이트
        LocalDateTime now = LocalDateTime.now();
        int updatedCount = chatRepository.markMessagesAsRead(chatRoomId, currentUserId, now);

        String readAtStr = (updatedCount > 0)
                ? now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : null;

        // 4. 응답 DTO 생성 후 반환
        return ChatReadResponseDTO.builder()
                .chatRoomId(chatRoomId)
                .updatedCount(updatedCount)
                .readAt(readAtStr)
                .build();
    }

    /**
     * 채팅방 참여자인지 확인
     * user1 또는 user2 중 하나라도 일치하면 true
     */
    private boolean isParticipant(ChatRoom chatRoom, Long userId) {
        return (chatRoom.getUser1() != null && chatRoom.getUser1().getId().equals(userId))
                || (chatRoom.getUser2() != null && chatRoom.getUser2().getId().equals(userId));
    }
}