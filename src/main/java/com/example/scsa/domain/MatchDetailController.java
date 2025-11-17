package com.example.scsa.domain;

import com.example.scsa.domain.dto.MatchDetailDTO;
import com.example.scsa.domain.service.MatchDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/matches")
@RequiredArgsConstructor
public class MatchDetailController {

    private final MatchDetailService matchDetailService;

    @GetMapping("/{match_id}")
    public MatchDetailDTO getMatch(@PathVariable("match_id") Long matchId){
        return matchDetailService.findByMatchId(matchId);
    }

}
