package com.example.scsa.exception.chat;

import com.example.scsa.exception.ErrorCode;

public class ChatRoomNotFoundException extends RuntimeException {
    public ChatRoomNotFoundException(String message) {
        super(message);
    }

    public ChatRoomNotFoundException(Long chatRoomId) {
        super("채팅방이 존재하지 않습니다. chatRoomId=" + chatRoomId);
    }

    public ChatRoomNotFoundException(){
        super(ErrorCode.CHAT_ROOM_NOT_FOUND.getMessage());
    }
}
