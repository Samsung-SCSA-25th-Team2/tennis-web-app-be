package com.example.scsa.exception;

public class InvalidCursorFormatException extends RuntimeException {
    public InvalidCursorFormatException(String cursor) {
        super("잘못된 cursor 형식입니다: " + cursor);
    }
}
