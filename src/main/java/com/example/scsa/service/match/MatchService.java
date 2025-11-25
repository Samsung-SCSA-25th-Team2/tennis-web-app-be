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
import com.example.scsa.exception.*;
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
                .playerCountMen(dto.getPlayerCountMen())
                .playerCountWomen(dto.getPlayerCountWomen())
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

    @Transactional
    public void deleteMatch(Long hostId, Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException());

        if (!match.getHost().getId().equals(hostId)){
            throw new MatchAccessDeniedException(matchId);
        }

        matchRepository.delete(match);
    }

    @Transactional
    public MatchResponseDTO changeMatchStatus(Long matchId, Long currentUserId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new MatchNotFoundException(matchId));

        // 제약 1: 본인이 작성한 매치인지 확인 (host만 변경 가능)
        if (!match.getHost().getId().equals(currentUserId)) {
            throw new MatchAccessDeniedException(matchId, currentUserId);
        }

        LocalDateTime now = LocalDateTime.now();
        MatchStatus currentStatus = match.getMatchStatus();

        // 제약 2:
        //  - 매치 시작 시간이 현재보다 이전인데
        //  - 현재 상태가 COMPLETED라면, 다시 RECRUITING으로 되돌리는 것은 금지
        if (match.getMatchStartDateTime().isBefore(now)
                && currentStatus == MatchStatus.COMPLETED) {
            throw new InvalidMatchStatusChangeException(
                    "이미 시작된 매치는 COMPLETED → RECRUITING으로 변경할 수 없습니다."
            );
        }

        // 토글 로직: RECRUITING <-> COMPLETED
        MatchStatus newStatus =
                (currentStatus == MatchStatus.RECRUITING)
                        ? MatchStatus.COMPLETED
                        : MatchStatus.RECRUITING;

        match.updateMatchStatus(newStatus); // JPA 더티체킹으로 업데이트

        return new MatchResponseDTO(matchId, "매치가 성공적으로 변경되었습니다.");
    }
}
