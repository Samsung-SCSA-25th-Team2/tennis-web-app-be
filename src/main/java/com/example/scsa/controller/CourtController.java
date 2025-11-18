package com.example.scsa.controller;


import com.example.scsa.domain.service.CourtSearchService;
import com.example.scsa.domain.service.CourtService;
import com.example.scsa.dto.CourtDTO;
import com.example.scsa.dto.CourtSearchDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tennis-courts")
public class CourtController {

    private final CourtService courtService;
    private final CourtSearchService courtSearchService;

    /**
     * 테니스장 단건 조회
     * GET /api/v1/tennis-courts/{court_id}
     */
    @GetMapping("/{court_id}")
    public ResponseEntity<CourtDTO> getCourt(
            @PathVariable("court_id") Long courtId) {

        CourtDTO response = courtService.findById(courtId);
        return ResponseEntity.ok(response);  // 200
    }

    /**
     * keyword를 통한 테니스장 검색
     * GET /api/v1/tennis-courts/search?keyword={keyword}
     */
    @GetMapping("/search")
    public ResponseEntity<CourtSearchDTO> searchCourts(
            @RequestParam("keyword") String keyword){

        CourtSearchDTO result = courtSearchService.searchByKeyword(keyword);
        return ResponseEntity.ok(result);
    }
}