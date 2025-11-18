package com.example.scsa.exception;

public class CourtNotFoundException extends RuntimeException {
    public CourtNotFoundException(Long courtId) {
        super("입력하신 경기장이 없습니다.");
    }
}
