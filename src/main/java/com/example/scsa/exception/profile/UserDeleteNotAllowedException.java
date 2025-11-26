package com.example.scsa.exception.profile;

import com.example.scsa.exception.ErrorCode;

public class UserDeleteNotAllowedException extends RuntimeException {
    public UserDeleteNotAllowedException(String message) {
        super(message);
    }

    public UserDeleteNotAllowedException(){
        super(ErrorCode.USER_DELETE_NOT_ALLOWED.getMessage());
    }
}