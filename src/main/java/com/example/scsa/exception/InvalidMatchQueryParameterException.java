package com.example.scsa.exception;

import com.example.scsa.exception.ErrorCode;

public class InvalidMatchQueryParameterException extends RuntimeException {

    public InvalidMatchQueryParameterException(){
        super(ErrorCode.INVALID_PROFILE_UPDATE.getMessage());
    }
    public InvalidMatchQueryParameterException(String message) {
        super(message);
    }
}