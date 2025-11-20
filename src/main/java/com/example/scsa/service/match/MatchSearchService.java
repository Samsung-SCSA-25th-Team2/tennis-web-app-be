package com.example.scsa.service.match;

import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.entity.MatchGuest;
import com.example.scsa.domain.entity.User;
import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Period;
import com.example.scsa.dto.match.MatchResponseDTO;
import com.example.scsa.dto.match.MatchSearchDTO;
import com.example.scsa.exception.MatchNotFoundException;
import com.example.scsa.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchSearchService {

    private final MatchRepository matchRepository;

    @Transactional(readOnly = true)
    public MatchSearchDTO searchMatch(Long matchId){
        Match match = matchRepository.findByIdWithGuests(matchId)
                .orElseThrow(() -> new MatchNotFoundException());


        for (MatchGuest matchGuest : match.getMatchGuests()) {
            User user = matchGuest.getUser();
        }

        List<String> periods = match.getPeriods().stream()
                .sorted(Comparator.comparing(Enum::ordinal))
                .map(Period::name)
                .toList();

        List<String> ages = match.getAges().stream()
                .sorted(Comparator.comparing(Enum::ordinal))
                .map(Age::name)
                .toList();

        String status = switch (match.getMatchStatus()) {
            case RECRUITING -> "OPEN";
            case COMPLETED -> "CLOSED";
        };

        return MatchSearchDTO.builder()
                .matchId(match.getId())
                .hostId(match.getHost().getId())
                .startDateTime(toIso(match.getMatchStartDateTime()))
                .endDateTime(toIso(match.getMatchEndDateTime()))
                .gameType(match.getGameType().name())
                .courtId(match.getCourt().getId())
                .period(periods)
                .playerCountMen(match.getPlayerCountMen())
                .playerCountWomen(match.getPlayerCountWomen())
                .ageRange(ages)
                .fee(match.getFee())
                .description(match.getDescription())
                .status(status)
                .createdAt(toIso(match.getCreatedAt()))
                .updatedAt(toIso(match.getLastModifiedAt()))
                .build();


    }

    private String toIso(LocalDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
    }


}
