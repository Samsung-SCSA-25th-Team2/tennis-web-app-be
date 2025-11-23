package com.example.scsa.exception;

public class InvalidChatMessageException extends RuntimeException {

    public InvalidChatMessageException(String message) {
        super(message);
    }
    public InvalidChatMessageException(){
        super(ErrorCode.INVALID_CHAT_MESSAGE.getMessage());
    }
}