package com.example.scsa.controller;


import com.example.scsa.service.court.CourtSearchService;
import com.example.scsa.service.court.CourtService;
import com.example.scsa.dto.court.CourtDTO;
import com.example.scsa.dto.court.CourtSearchDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tennis-courts")
@Tag(name = "테니스장 API", description = "테니스장 조회 및 검색 관련 API")
public class CourtController {

    private final CourtService courtService;
    private final CourtSearchService courtSearchService;

    /**
     * 테니스장 단건 조회
     * GET /api/v1/tennis-courts/{court_id}
     */
    @Operation(summary = "테니스장 상세 조회", description = "테니스장 ID로 특정 테니스장의 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공",
            content = @Content(schema = @Schema(implementation = CourtDTO.class)))
    })
    @GetMapping("/{court_id}")
    public ResponseEntity<CourtDTO> getCourt(
            @Parameter(description = "테니스장 ID", required = true) @PathVariable("court_id") Long courtId) {

        CourtDTO response = courtService.findById(courtId);
        return ResponseEntity.ok(response);  // 200
    }

    /**
     * keyword를 통한 테니스장 검색
     * GET /api/v1/tennis-courts/search?keyword={keyword}
     */
    @Operation(summary = "테니스장 검색", description = "키워드로 테니스장을 검색합니다. 페이징을 지원합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "검색 성공",
            content = @Content(schema = @Schema(implementation = CourtSearchDTO.class)))
    })
    @GetMapping("/search")
    public ResponseEntity<CourtSearchDTO> searchCourts(
            @Parameter(description = "검색 키워드", required = true) @RequestParam("keyword") String keyword,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0") @RequestParam(value = "page", defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10") @RequestParam(value = "size", defaultValue = "10") int size){

        CourtSearchDTO result = courtSearchService.searchByKeyword(keyword, page, size);
        return ResponseEntity.ok(result);
    }
}