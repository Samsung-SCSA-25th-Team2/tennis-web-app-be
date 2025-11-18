package com.example.scsa.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidProfileUpdateException extends RuntimeException {

    public InvalidProfileUpdateException(String message) {
        super(message);
    }

    public InvalidProfileUpdateException() {
        super("잘못된 프로필 수정 요청입니다.");
    }
}
