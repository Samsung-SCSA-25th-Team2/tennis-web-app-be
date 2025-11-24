package com.example.scsa.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReadResponseDTO {

    /**
     * 읽음 처리한 채팅방 ID
     */
    private Long chatRoomId;

    /**
     * 이번 요청으로 새롭게 읽음 처리된 메시지 개수
     */
    private int updatedCount;

    /**
     * 서버 기준 처리 시각 (ISO-8601 문자열),
     * updatedCount == 0 이면 null
     */
    private String readAt;
}