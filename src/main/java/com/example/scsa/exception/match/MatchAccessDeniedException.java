package com.example.scsa.exception.match;

import com.example.scsa.exception.ErrorCode;

/**
 * 경기를 찾을 수 없을 때 발생하는 예외
 */
public class MatchAccessDeniedException extends RuntimeException {

    public MatchAccessDeniedException() {
        super(ErrorCode.MATCH_ACCESS_DENIED.getMessage());
    }

    public MatchAccessDeniedException(String message) {
        super(message);
    }

    public MatchAccessDeniedException(Long matchId) {
        super("접근 권한이 없습니다. (ID: " + matchId + ")");
    }

    public MatchAccessDeniedException(Long matchId, Long userId) {
        super("접근 권한이 없습니다. (ID: " + matchId + ", userId: " + userId + ")");
    }
}