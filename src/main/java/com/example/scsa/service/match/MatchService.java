package com.example.scsa.service.match;

import com.example.scsa.domain.entity.Court;
import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.entity.User;
import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.GameType;
import com.example.scsa.domain.vo.MatchStatus;
import com.example.scsa.domain.vo.Period;
import com.example.scsa.dto.match.MatchDTO;
import com.example.scsa.dto.match.MatchResponseDTO;
import com.example.scsa.exception.CourtNotFoundException;
import com.example.scsa.exception.UserNotFoundException;
import com.example.scsa.repository.CourtRepository;
import com.example.scsa.repository.MatchRepository;
import com.example.scsa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;

    @Transactional
    public MatchResponseDTO createMatch(Long hostId, MatchDTO dto){
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new UserNotFoundException());

        Court court = courtRepository.findById(dto.getCourtId())
                .orElseThrow(() -> new CourtNotFoundException());

        LocalDateTime start = LocalDateTime.parse(dto.getStartDateTime());
        LocalDateTime end = LocalDateTime.parse(dto.getEndDateTime());

        if (end.isBefore(start)){
            throw new IllegalArgumentException("종료 시간은 시작 시간 이후이어야 합니다.");
        }
        Match match = Match.builder()
                .host(host)
                .court(court)
                .matchStartDateTime(start)
                .matchEndDateTime(end)
                .gameType(GameType.valueOf(dto.getGameType()))
                .matchStatus(MatchStatus.RECRUITING)
                .fee(dto.getFee())
                .description(dto.getDescription())
                .build();

        dto.getAgeRange().forEach(ageStr -> {
            Age age = Age.valueOf(ageStr);
            match.addAge(age);
        });

        dto.getPeriod().forEach(periodStr -> {
            Period period = Period.valueOf(periodStr); // "2" → Period.P2, 등
            match.addPeriod(period);
        });

        Match saved = matchRepository.save(match);

        return new MatchResponseDTO(saved.getId(), "매치가 성공적으로 등록되었습니다.");
    }
}
