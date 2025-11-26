package com.example.scsa.exception.chat;

import com.example.scsa.exception.ErrorCode;

public class ChatRoomAlreadyExistsException extends RuntimeException {

    public ChatRoomAlreadyExistsException(Long matchId, Long hostId, Long guestId) {
        super("채팅방이 이미 존재합니다. matchId =" + matchId
                + ", hostId=" + hostId
                + ", guestId=" + guestId);
    }

    public ChatRoomAlreadyExistsException(String message){
        super(message);
    }

    public ChatRoomAlreadyExistsException() {
        super(ErrorCode.CHAT_ROOM_ALREADY_EXISTS.getMessage());
    }
}