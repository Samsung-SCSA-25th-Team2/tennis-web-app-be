package com.example.scsa.exception.profile;

public class UserDeleteNotAllowedException extends RuntimeException {
    public UserDeleteNotAllowedException(String message) {
        super(message);
    }
}