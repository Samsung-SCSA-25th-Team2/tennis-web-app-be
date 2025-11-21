package com.example.scsa.dto.chat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatRoomDTO {
    private Long chatRoomId;
    private Long matchId;

    private Long opponentId;
    private String opponentNickname;
    private String opponentImgUrl;

    private String lastMessagePreview;
    private String lastMessageAt;

    private int unreadCount;
}
