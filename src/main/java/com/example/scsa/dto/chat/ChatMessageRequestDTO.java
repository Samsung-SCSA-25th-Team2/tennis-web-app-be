package com.example.scsa.dto.chat;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequestDTO {
    private Long chatRoomId;
    private Long senderId;
    private String message; // 메시지 본문
}
