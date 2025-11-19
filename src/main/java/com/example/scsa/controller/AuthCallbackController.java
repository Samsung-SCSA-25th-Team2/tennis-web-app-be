package com.example.scsa.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * OAuth2 콜백 페이지 Controller
 */
@Controller
public class AuthCallbackController {

    /**
     * OAuth2 로그인 성공 후 콜백 페이지
     * static/auth/callback.html을 반환
     */
    @GetMapping("/auth/callback")
    public String authCallback() {
        return "forward:/auth/callback.html";
    }
}
