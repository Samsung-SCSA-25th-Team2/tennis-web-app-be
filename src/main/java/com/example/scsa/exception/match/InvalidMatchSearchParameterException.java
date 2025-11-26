package com.example.scsa.exception.match;

import com.example.scsa.exception.ErrorCode;

public class InvalidMatchSearchParameterException extends RuntimeException {

    public InvalidMatchSearchParameterException(String message) {
        super(message);
    }

    public InvalidMatchSearchParameterException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidMatchSearchParameterException(){
        super(ErrorCode.INVALID_MATCH_SEARCH_PARAMETER.getMessage());
    }
}
