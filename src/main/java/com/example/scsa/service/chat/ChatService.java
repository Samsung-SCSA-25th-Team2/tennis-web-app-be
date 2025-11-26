package com.example.scsa.service.chat;

import com.example.scsa.domain.entity.Chat;
import com.example.scsa.domain.entity.ChatRoom;
import com.example.scsa.domain.entity.User;
import com.example.scsa.dto.chat.ChatMessageRequestDTO;
import com.example.scsa.dto.chat.ChatMessageResponseDTO;
import com.example.scsa.exception.InvalidChatMessageException;
import com.example.scsa.repository.ChatRepository;
import com.example.scsa.repository.ChatRoomRepository;
import com.example.scsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatRepository chatRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessageResponseDTO saveChat(ChatMessageRequestDTO dto) {

        validate(dto);

        ChatRoom chatRoom = chatRoomRepository.findById(dto.getChatRoomId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 채팅방입니다. id=" + dto.getChatRoomId()));

        User sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + dto.getSenderId()));

        Chat chat = new Chat(chatRoom, sender, dto.getMessage());
        Chat saved = chatRepository.save(chat);

        // ChatRoom의 마지막 메시지 정보 갱신
        // createdAt이 Auditing으로 관리된다면 saved.getCreatedAt() 사용
        // 아니라면 LocalDateTime.now() 사용
        chatRoom.updateLastMessage(dto.getMessage(), saved.getCreatedAt());

        // 별도의 save 호출 불필요 (영속 상태라 트랜잭션 커밋 시 자동 flush)
        // chatRoomRepository.save(chatRoom);  // 일반적으로 안 해도 됨
        return ChatMessageResponseDTO.from(saved);
    }

    public List<ChatMessageResponseDTO> getChatMessages(Long chatRoomId) {
        return chatRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId)
                .stream()
                .map(ChatMessageResponseDTO::from)
                .toList();
    }

    private void validate(ChatMessageRequestDTO dto) {
        if (dto.getChatRoomId() == null) {
            throw new InvalidChatMessageException("chatRoomId는 필수입니다.");
        }
        if (dto.getSenderId() == null) {
            throw new InvalidChatMessageException("senderId는 필수입니다.");
        }
        if (dto.getMessage() == null || dto.getMessage().isBlank()) {
            throw new InvalidChatMessageException("메시지는 비어 있을 수 없습니다.");
        }
    }
}
