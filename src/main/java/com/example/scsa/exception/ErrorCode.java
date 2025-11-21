package com.example.scsa.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COMMON-001", "잘못된 입력 값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON-002", "서버 오류가 발생했습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COMMON-003", "지원하지 않는 HTTP 메서드입니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "COMMON-004", "잘못된 타입입니다."),
    MISSING_REQUEST_PARAMETER(HttpStatus.BAD_REQUEST, "COMMON-005", "필수 파라미터가 누락되었습니다."),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-001", "존재하지 않는 회원입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "USER-002", "이미 사용 중인 닉네임입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "USER-003", "인증되지 않은 사용자입니다."),
    INVALID_PROFILE_UPDATE(HttpStatus.BAD_REQUEST, "USER-004", "잘못된 프로필 수정 요청입니다."),

    // Court
    COURT_NOT_FOUND(HttpStatus.NOT_FOUND, "COURT-001", "존재하지 않는 테니스장입니다."),

    // Match
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "MATCH-001", "존재하지 않는 경기입니다."),

    //Chat
    CHAT_ROOM_ALREADY_EXISTS(HttpStatus.CONFLICT, "CHAT-001", "이미 존재하는 채팅방입니다."),

    // Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-001", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH-002", "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "AUTH-003", "Refresh Token이 없습니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "AUTH-004", "Refresh Token이 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}