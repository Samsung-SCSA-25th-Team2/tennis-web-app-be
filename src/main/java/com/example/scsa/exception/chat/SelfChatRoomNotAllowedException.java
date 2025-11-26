package com.example.scsa.exception.chat;

import com.example.scsa.exception.ErrorCode;

public class SelfChatRoomNotAllowedException extends RuntimeException {
    public SelfChatRoomNotAllowedException(String message) {
        super(message);
    }

    public SelfChatRoomNotAllowedException(Long userId){
        super("채팅방의 호스트와 게스트는 동일할 수 없습니다. userId = "+  userId);
    }

    public SelfChatRoomNotAllowedException(){
        super(ErrorCode.SELF_CHAT_ROOM_NOT_ALLOWED.getMessage());
    }
}
