package com.example.scsa.domain.vo;

/**
 * 사용자 권한
 * Spring Security의 GrantedAuthority에서 사용
 */
public enum Role {
    USER,    // 일반 사용자
    ADMIN    // 관리자
}