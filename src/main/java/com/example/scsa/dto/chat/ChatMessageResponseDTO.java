package com.example.scsa.dto.chat;

import com.example.scsa.domain.entity.Chat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponseDTO {

    private Long chatId;
    private Long chatRoomId;
    private Long senderId;
    private String senderNickname;
    private String message;

    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public static ChatMessageResponseDTO from(Chat chat) {
        return ChatMessageResponseDTO.builder()
                .chatId(chat.getId())
                .chatRoomId(chat.getChatRoom().getId())
                .senderId(chat.getSender().getId())
                .senderNickname(chat.getSender().getNickname())
                .message(chat.getMessage())
                .isRead(chat.getIsRead())
                .createdAt(chat.getCreatedAt())
                .readAt(chat.getReadAt())
                .build();
    }
}
