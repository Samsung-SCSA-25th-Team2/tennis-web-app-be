package com.example.scsa.service.match;

import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.entity.MatchGuest;
import com.example.scsa.domain.entity.User;
import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Period;
import com.example.scsa.dto.match.MatchSearchDTO;
import com.example.scsa.exception.match.MatchNotFoundException;
import com.example.scsa.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchSearchService {

    private final MatchRepository matchRepository;

    /**
     * 매치 단건 상세 조회
     *
     * 실행 흐름:
     *  1) matchId로 매치 조회 (게스트 정보까지 fetch joining)
     *  2) 매치가 없으면 MatchNotFoundException 발생
     *  3) Period / Age Enum 컬렉션 정렬 후 문자열 리스트로 변환
     *  4) MatchStatus → OPEN/CLOSED 변환
     *  5) Match 엔티티 → MatchSearchDTO로 매핑 후 반환
     *
     * 주의:
     *  - findByIdWithGuests()는 MatchGuest까지 함께 가져오는 커스텀 조회 메서드이다.
     *  - 서비스 레이어에서는 비즈니스 로직 확인 및 DTO 변환만 수행한다.
     */
    @Transactional(readOnly = true)
    public MatchSearchDTO searchMatch(Long matchId) {

        // 1. 매치 + 게스트 목록 조회
        Match match = matchRepository.findByIdWithGuests(matchId)
                .orElseThrow(MatchNotFoundException::new);

        // 2. 게스트 리스트를 순회하여 Lazy loading이 안 일어나도록 미리 로딩
        for (MatchGuest matchGuest : match.getMatchGuests()) {
            User user = matchGuest.getUser();
        }

        // 3. Period enum → 문자열 리스트 (ordinal 기준 정렬)
        List<String> periods = match.getPeriods().stream()
                .sorted(Comparator.comparing(Enum::ordinal))
                .map(Period::name)
                .toList();

        // 4. Age enum → 문자열 리스트 (ordinal 기준 정렬)
        List<String> ages = match.getAges().stream()
                .sorted(Comparator.comparing(Enum::ordinal))
                .map(Age::name)
                .toList();

        // 5. MatchStatus → OPEN/CLOSED 변환
        String status = switch (match.getMatchStatus()) {
            case RECRUITING -> "OPEN";
            case COMPLETED -> "CLOSED";
        };

        // 6. DTO 변환 및 반환
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

    /**
     * LocalDateTime → ISO-8601 문자열 변환 (Z 없이)
     * 프론트엔드 규약 변경: 2025-11-17T19:00:00
     */
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private String toIso(LocalDateTime dt) {
        return dt != null ? dt.format(ISO_FORMATTER) : null;
    }
}