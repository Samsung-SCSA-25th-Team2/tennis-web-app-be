package com.example.scsa.exception.chat;

import com.example.scsa.exception.ErrorCode;

public class InvalidChatMessageException extends RuntimeException {

    public InvalidChatMessageException(String message) {
        super(message);
    }
    public InvalidChatMessageException(){
        super(ErrorCode.INVALID_CHAT_MESSAGE.getMessage());
    }
}