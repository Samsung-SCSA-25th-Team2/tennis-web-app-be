package com.example.scsa.exception;

/**
 * 테니스장을 찾을 수 없을 때 발생하는 예외
 */
public class CourtNotFoundException extends RuntimeException {

    public CourtNotFoundException() {
        super(ErrorCode.COURT_NOT_FOUND.getMessage());
    }

    public CourtNotFoundException(String message) {
        super(message);
    }

    public CourtNotFoundException(Long courtId) {
        super("테니스장을 찾을 수 없습니다. (ID: " + courtId + ")");
    }
}
