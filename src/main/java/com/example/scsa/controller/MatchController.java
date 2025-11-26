package com.example.scsa.controller;

import com.example.scsa.dto.match.*;
import com.example.scsa.dto.response.ErrorResponse;
import com.example.scsa.exception.match.InvalidMatchSearchParameterException;
import com.example.scsa.exception.match.MatchNotFoundException;
import com.example.scsa.service.match.MatchListService;
import com.example.scsa.service.match.MatchSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/matches")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "매치 API (인증 필요 X)", description = "매치 리스트 및 상세 조회 관련 API")
public class MatchController {

    private final MatchSearchService matchSearchService;
    private final MatchListService matchListService;

    /**
     * 매치 단건 조회
     * GET /api/v1/matches/{match_id}
     */
    @Operation(
            summary = "매치 상세 조회",
            description = "match_id로 특정 매치의 상세 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MatchSearchDTO.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "매치를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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

    /**
     * 매치 리스트 조회
     * GET /api/v1/matches?{...}
     */
    @Operation(
            summary = "매치 리스트 조회",
            description = "필터 및 정렬 조건을 바탕으로 매치 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MatchListResponseDTO.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 조회 파라미터",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
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