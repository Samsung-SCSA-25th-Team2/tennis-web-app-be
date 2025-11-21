package com.example.scsa.exception;

public class ChatAccessDeniedException extends RuntimeException {

    public ChatAccessDeniedException(Long chatRoomId, Long userId) {
        super("채팅방 접근 권한이 없습니다. chatRoomId=" + chatRoomId + ", userId=" + userId);
    }
}