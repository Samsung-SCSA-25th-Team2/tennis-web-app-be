package com.example.scsa.exception.chat;

import com.example.scsa.exception.ErrorCode;

public class ChatRoomAccessDeniedException extends RuntimeException {
    public ChatRoomAccessDeniedException(Long chatRoomId, Long userId) {
        super("채팅방의 참가자가 아닙니다. chatRoomId=" + chatRoomId + ", userId=" + userId);
    }

    public ChatRoomAccessDeniedException(){
        super(ErrorCode.CHAT_ROOM_ACCESS_DENIED.getMessage());
    }
}
