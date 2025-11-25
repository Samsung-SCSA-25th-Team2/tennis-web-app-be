package com.example.scsa.controller;

import com.example.scsa.dto.match.*;
import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.exception.InvalidMatchSearchParameterException;
import com.example.scsa.exception.InvalidMatchStatusChangeException;
import com.example.scsa.exception.MatchAccessDeniedException;
import com.example.scsa.exception.MatchNotFoundException;
import com.example.scsa.service.match.MatchListService;
import com.example.scsa.service.match.MatchMyListService;
import com.example.scsa.service.match.MatchSearchService;
import com.example.scsa.service.match.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/me/matches")
@RequiredArgsConstructor
@Slf4j
public class MyMatchController {

    private final MatchService matchService;
    private final MatchMyListService matchMyListService;

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

    @DeleteMapping("/{match_id}")
    public ResponseEntity<?> deleteMatch(@PathVariable("match_id") Long matchId){

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 401 처리: 인증 안 된 경우
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        try {
            Long hostId = Long.parseLong(authentication.getName());
            log.info("매치 상태 변경 요청 - hostId: {}, matchId: {}", hostId, matchId);

            matchService.deleteMatch(hostId, matchId);
            log.info("매치 상태 변경 성공 - hostId: {}, matchId: {}", hostId, matchId);

            return ResponseEntity.ok("매치가 성공적으로 삭제되었습니다.");
        } catch (MatchNotFoundException e) {
            log.warn("매치 상태 변경 실패 - 존재하지 않는 매치, matchId: {}", matchId);
            return ResponseEntity.status(404)
                    .body(ErrorResponse.of("요청한 매치를 찾을 수 없습니다.", "MATCH_NOT_FOUND"));

        } catch (MatchAccessDeniedException e) {
            log.warn("매치 상태 변경 실패 - 권한 없음, matchId: {}, userId: {}", matchId, authentication.getName());
            return ResponseEntity.status(403)
                    .body(ErrorResponse.of("해당 매치를 삭제할 권한이 없습니다.", "FORBIDDEN"));

        } catch (Exception e) {
            log.error("매치 상태 변경 실패 - 서버오류 : {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

    @PatchMapping("/{match_id}")
    public ResponseEntity<?> changeMatchStatus(@PathVariable("match_id") Long matchId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 401 처리: 인증 안 된 경우
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        try {
            Long hostId = Long.parseLong(authentication.getName());
            log.info("매치 상태 변경 요청 - hostId: {}, matchId: {}", hostId, matchId);

            MatchResponseDTO response = matchService.changeMatchStatus(matchId, hostId);
            log.info("매치 상태 변경 성공 - hostId: {}, matchId: {}", hostId, matchId);

            return ResponseEntity.ok(response);
        } catch(InvalidMatchStatusChangeException e) {
            log.error("유효하지 않은 매치 변경", e.getMessage(), e);
            return ResponseEntity.status(400)
                    .body(ErrorResponse.of("유효하지 않은 매치 변경", "INVALID_MATCH_CHANGE_STATUS"));
        } catch (MatchNotFoundException e) {
            log.warn("매치 상태 변경 실패 - 존재하지 않는 매치, matchId: {}", matchId);
            return ResponseEntity.status(404)
                    .body(ErrorResponse.of("요청한 매치를 찾을 수 없습니다.", "MATCH_NOT_FOUND"));
        } catch (MatchAccessDeniedException e) {
            log.warn("매치 상태 변경 실패 - 권한 없음, matchId: {}, userId: {}", matchId, authentication.getName());
            return ResponseEntity.status(403)
                    .body(ErrorResponse.of("해당 매치를 삭제할 권한이 없습니다.", "FORBIDDEN"));
        } catch (Exception e) {
            log.error("매치 상태 변경 실패 - 서버오류 : {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getMyMatches( @RequestParam(value = "cursor", required = false) Long cursor,
                                           @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 401 처리: 인증 안 된 경우
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return ResponseEntity.status(401)
                    .body(ErrorResponse.of("인증되지 않은 사용자입니다.", "UNAUTHORIZED"));
        }

        try {
            Long hostId = Long.parseLong(authentication.getName());
            log.info("내 매치 조회 요청 - hostId: {}", hostId);

            MatchMyListResponseDTO response = matchMyListService.getMyMatches(hostId, cursor, size);
            log.info("매치 상태 변경 성공 - hostId: {}", hostId);

            return ResponseEntity.ok(response);
        } catch(InvalidMatchStatusChangeException e) {
            log.error("유효하지 않은 매치 변경", e.getMessage(), e);
            return ResponseEntity.status(400)
                    .body(ErrorResponse.of("유효하지 않은 매치 변경", "INVALID_MATCH_CHANGE_STATUS"));
        } catch (Exception e) {
            log.error("매치 상태 변경 실패 - 서버오류 : {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }
}