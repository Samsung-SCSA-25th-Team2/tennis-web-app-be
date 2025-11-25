package com.example.scsa.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistoryResponseDTO {

    private List<MessageItem> messages;

    private String nextCursor;   // 가장 오래된 메시지 createdAt
    private boolean hasNext;

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MessageItem {
        private Long chatId;
        private Long chatRoomId;

        private Long senderId;
        private String senderNickname;
        private String senderImgUrl;

        private String message;

        private String createdAt;

        private boolean isRead;
        private String readAt;

        private boolean isMine;
    }
}
