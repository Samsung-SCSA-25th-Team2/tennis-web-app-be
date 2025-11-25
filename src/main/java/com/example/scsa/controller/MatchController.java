package com.example.scsa.controller;

import com.example.scsa.dto.match.*;
import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.exception.InvalidMatchSearchParameterException;
import com.example.scsa.exception.MatchNotFoundException;
import com.example.scsa.service.match.MatchListService;
import com.example.scsa.service.match.MatchSearchService;
import com.example.scsa.service.match.MatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/matches")
@RequiredArgsConstructor
@Slf4j
public class MatchController {

    private final MatchService matchService;
    private final MatchSearchService matchSearchService;
    private final MatchListService matchListService;

    @GetMapping("/{match_id}")
    public ResponseEntity<?> searchMatch(@PathVariable("match_id") Long matchId) {
        try {
            log.info("매치 상세 조회 요청 - matchId: {}", matchId);

            MatchSearchDTO dto = matchSearchService.searchMatch(matchId);

            log.info("매치 상세 조회 성공 - matchId: {}", matchId);
            return ResponseEntity.ok(dto);

        } catch (MatchNotFoundException e) {
            log.warn("매치 상세 조회 실패 - 존재하지 않는 matchId: {}", matchId);
            return ResponseEntity.status(404)
                    .body(ErrorResponse.of("요청한 매치를 찾을 수 없습니다.", "MATCH_NOT_FOUND"));

        } catch (Exception e) {
            log.error("매치 상세 조회 중 서버 에러 발생 - matchId: {}", matchId, e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getMatches(@ModelAttribute MatchListRequestDTO request) {
        try {
            MatchListResponseDTO response = matchListService.getMatchList(request);
            return ResponseEntity.ok(response);
        } catch(InvalidMatchSearchParameterException e){
            log.error("잘못된 매치 조회 : {}",e.getMessage(), e);
            return ResponseEntity.status(400)
                    .body(ErrorResponse.of("잘못된 매치 조회", "INVALID_MATCH_SEARCH_PARAMETER"));
        } catch(Exception e){
            log.error("매치 조회 실패 - 서버오류 : {}",  e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(ErrorResponse.of("서버 내부 오류가 발생했습니다.", "INTERNAL_SERVER_ERROR"));
        }
    }

}