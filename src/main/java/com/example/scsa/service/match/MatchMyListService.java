package com.example.scsa.service.match;

import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.vo.Age;
import com.example.scsa.domain.vo.Period;
import com.example.scsa.dto.match.MatchMyListResponseDTO;
import com.example.scsa.dto.match.MatchSearchDTO;
import com.example.scsa.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchMyListService {
    private final MatchRepository matchRepository;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    /**
     * 내가 호스트인 매치 목록 조회 (cursor 기반)
     *
     * @param hostId 현재 로그인 유저 ID
     * @param cursor 마지막 matchId (null 이면 첫 페이지)
     * @param size   페이지 크기
     */
    public MatchMyListResponseDTO getMyMatches(Long hostId, Long cursor, Integer size) {

        int pageSize = normalizeSize(size);

        Pageable pageable = PageRequest.of(0, pageSize);

        Slice<Match> slice;
        if (cursor == null) {
            slice = matchRepository.findByHostIdOrderByIdDesc(hostId, pageable);
        } else {
            slice = matchRepository.findByHostIdAndIdLessThanOrderByIdDesc(hostId, cursor, pageable);
        }

        List<MatchSearchDTO> content = slice.getContent().stream()
                .map(this::toDto)
                .toList();

        Long nextCursor = content.isEmpty() ? null : content.get(content.size() - 1).getMatchId();

        return MatchMyListResponseDTO.builder()
                .matches(content)
                .size(Long.valueOf(content.size()))
                .hasNext(slice.hasNext())
                .cursor(nextCursor)
                .build();
    }

    private int normalizeSize(Integer size) {
        if (size == null) return DEFAULT_SIZE;
        if (size <= 0) throw new IllegalArgumentException("size는 1 이상이어야 합니다.");
        return Math.min(size, MAX_SIZE);
    }

    private MatchSearchDTO toDto(Match match) {
        String status = switch (match.getMatchStatus()) {
            case RECRUITING -> "OPEN";
            case COMPLETED -> "CLOSED";
        };

        List<String> periods = match.getPeriods().stream()
                .map(Period::name)
                .toList();

        List<String> ages = match.getAges().stream()
                .map(Age::name)
                .toList();

        return MatchSearchDTO.builder()
                .matchId(match.getId())
                .hostId(match.getHost().getId())
                .startDateTime(match.getMatchStartDateTime().format(ISO_FORMATTER))
                .endDateTime(match.getMatchEndDateTime().format(ISO_FORMATTER))
                .gameType(match.getGameType().name())
                .courtId(match.getCourt().getId())
                .fee(match.getFee())
                .period(periods)
                .playerCountMen(match.getPlayerCountMen())
                .playerCountWomen(match.getPlayerCountWomen())
                .ageRange(ages)
                .status(status)
                .createdAt(match.getCreatedAt().format(ISO_FORMATTER))
                .build();
    }
}
