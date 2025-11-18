package com.example.scsa.exception;

public class MatchNotFoundException extends RuntimeException {
    public MatchNotFoundException(Long id) {
        super("조회하려는 경기가 없습니다.");
    }
}