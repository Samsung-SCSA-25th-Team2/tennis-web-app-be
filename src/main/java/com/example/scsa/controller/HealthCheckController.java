package com.example.scsa.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * CodeDeploy 및 ALB 헬스체크용 컨트롤러
 * JWT 인증, 전역 예외 처리와 완전히 분리된 간단한 엔드포인트
 */
@RestController
public class HealthCheckController {

    @GetMapping("/internal/health")
    public String healthCheck() {
        return "OK";
    }
}
