package com.example.scsa.domain.service;

import com.example.scsa.domain.dto.MatchDetailDTO;
import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.entity.MatchGuest;
import com.example.scsa.domain.exception.MatchNotFoundException;
import com.example.scsa.domain.vo.Gender;
import com.example.scsa.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchDetailService {

    private final MatchRepository matchRepository;

    @Transactional(readOnly=true)
    public MatchDetailDTO findByMatchId(Long matchId){
        Match match = matchRepository.findByIdWithGuests(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        List<MatchGuest> matchGuests = match.getMatchGuests();
        int cntMen = 0;
        int cntWomen = 0;
        int cntOther = 0;
        for (MatchGuest matchGuest : matchGuests){
            if (matchGuest.getUser().getGender().equals(Gender.MALE)) cntMen++;
            else if (matchGuest.getUser().getGender().equals(Gender.FEMALE)) cntWomen++;
            else cntOther++;
        }

        return MatchDetailDTO.builder()
                .matchId(match.getId())
                .hostId(match.getHost().getId())
                .startDate(toIso(match.getMatchStartDateTime()))
                .endDate(toIso(match.getMatchEndDateTime()))
                .gameType(null)
                .courtId(null)
                .level(null)
                .playerCountMen(cntMen)
                .playerCountWomen(cntWomen)
                .ageRange(match.getAges().stream().map(Enum::name).collect(Collectors.toList()).toString())
                .fee(match.getFee())
                .description(match.getDescription())
                .status(match.getMatchStatus().toString())
                .createdAt(toIso(match.getCreatedAt()))
                .updatedAt(toIso(match.getLastModifiedAt())).build();
    }

    private String toIso(LocalDateTime time){
        if (time == null) return null;
        return time.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

}
