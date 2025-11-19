package com.example.scsa.controller;

import com.example.scsa.dto.match.MatchDTO;
import com.example.scsa.dto.match.MatchResponseDTO;
import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.service.match.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/matches")
@RequiredArgsConstructor
@Slf4j
public class MatchController {

    private final MatchService matchService;

    @PostMapping
    public ResponseEntity<?> createMatch(@Valid @RequestBody MatchDTO request) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        try {
            Long hostId = Long.parseLong(authentication.getName());
            log.info("매치 등록 요청 - hostId: {}", hostId);

            MatchResponseDTO response = matchService.createMatch(hostId, request);

            log.info("매치 등록성공 - hostId: {}", hostId);
            return ResponseEntity.ok(response);

        } catch (jakarta.validation.ConstraintViolationException e) {
            return ResponseEntity.badRequest()
                    .body(ErrorResponse.of("요청 데이터가 유효하지 않습니다.", e.getMessage()));

        } catch (Exception e) {
            log.error("매치 등록 중 서버 에러 발생", e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 내부 오류", e.getMessage()));
        }
    }
}