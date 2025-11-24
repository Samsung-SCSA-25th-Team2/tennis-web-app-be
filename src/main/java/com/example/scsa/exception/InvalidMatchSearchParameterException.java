package com.example.scsa.exception;

public class InvalidMatchSearchParameterException extends RuntimeException {

    public InvalidMatchSearchParameterException(String message) {
        super(message);
    }

    public InvalidMatchSearchParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
