package com.example.scsa.service.chat;

import com.example.scsa.domain.entity.Chat;
import com.example.scsa.domain.entity.ChatRoom;
import com.example.scsa.domain.entity.User;
import com.example.scsa.dto.chat.ChatMessageRequestDTO;
import com.example.scsa.dto.chat.ChatMessageResponseDTO;
import com.example.scsa.exception.chat.InvalidChatMessageException;
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

    /**
     * 채팅 메시지를 데이터베이스에 저장하는 메서드.
     *
     * 실행 흐름:
     *  1) DTO 값 검증 (chatRoomId, senderId, message 필수)
     *  2) 채팅방 존재 여부 확인
     *  3) 사용자 존재 여부 확인
     *  4) Chat 엔티티 생성 및 저장
     *  5) 저장된 Chat을 ChatMessageResponseDTO로 변환해 반환
     *
     * 해당 메서드는 WebSocket에서 들어온 메시지를 DB에 기록하는 역할을 담당한다.
     */
    @Transactional
    public ChatMessageResponseDTO saveChat(ChatMessageRequestDTO dto) {

        // 1. 필수값 검증
        validate(dto);

        // 2. 채팅방 존재 확인
        ChatRoom chatRoom = chatRoomRepository.findById(dto.getChatRoomId())
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 채팅방입니다. id=" + dto.getChatRoomId())
                );

        // 3. 발신자 존재 확인
        User sender = userRepository.findById(dto.getSenderId())
                .orElseThrow(() ->
                        new IllegalArgumentException("존재하지 않는 사용자입니다. id=" + dto.getSenderId())
                );

        // 4. Chat 엔티티 생성 및 저장
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

    /**
     * 채팅 메시지 DTO의 필수값 검증
     *
     * - chatRoomId: 어느 채팅방인지 반드시 알아야 함
     * - senderId: 누가 보낸 메시지인지 반드시 필요함
     * - message: 비어있는 메시지는 허용하지 않음
     *
     * 클라이언트가 잘못된 요청을 보냈을 때 즉시 예외를 발생시킨다.
     */
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
