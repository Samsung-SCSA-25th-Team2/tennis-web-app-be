package com.example.scsa.exception;

/**
 * 경기를 찾을 수 없을 때 발생하는 예외
 */
public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException() {
        super(ErrorCode.MATCH_NOT_FOUND.getMessage());
    }

    public MatchNotFoundException(String message) {
        super(message);
    }

    public MatchNotFoundException(Long matchId) {
        super("경기를 찾을 수 없습니다. (ID: " + matchId + ")");
    }
}