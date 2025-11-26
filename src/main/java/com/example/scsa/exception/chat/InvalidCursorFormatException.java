package com.example.scsa.exception.chat;

import com.example.scsa.exception.ErrorCode;

public class InvalidCursorFormatException extends RuntimeException {
    public InvalidCursorFormatException(String cursor) {
        super("잘못된 cursor 형식입니다: " + cursor);
    }

    public InvalidCursorFormatException(){
        super(ErrorCode.INVALID_CURSOR_FORMAT.getMessage());
    }
}
