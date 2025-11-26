package com.example.scsa.exception.match;

import com.example.scsa.exception.ErrorCode;

public class InvalidMatchStatusChangeException extends RuntimeException {
    public InvalidMatchStatusChangeException(String message) {
        super(message);
    }

    public InvalidMatchStatusChangeException(){
        super(ErrorCode.INVALID_MATCH_STATUS_CHANGE.getMessage());
    }
}
