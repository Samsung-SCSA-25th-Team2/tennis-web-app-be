package com.example.scsa.exception.profile;

import com.example.scsa.exception.ErrorCode;

/**
 * 잘못된 프로필 수정 요청 시 발생하는 예외
 */
public class InvalidProfileUpdateException extends RuntimeException {

    public InvalidProfileUpdateException() {
        super(ErrorCode.INVALID_PROFILE_UPDATE.getMessage());
    }

    public InvalidProfileUpdateException(String message) {
        super("프로필을 업데이트 할 수 없습니다.");
    }
}
