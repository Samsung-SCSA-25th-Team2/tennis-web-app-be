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
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchMyListService {

    private final MatchRepository matchRepository;

    // Z 없이 응답 (프론트엔드 규약 변경: 2025-11-17T19:00:00)
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int DEFAULT_SIZE = 10;
    private static final int MAX_SIZE = 50;

    /**
     * 내가 호스트인 매치 목록 조회 (cursor 기반)
     *
     * 동작 방식:
     *  1) size 값 정규화 (null → 기본값, 0 이하 → 예외, MAX_SIZE로 상한 제한)
     *  2) cursor가 없으면: hostId 기준으로 id DESC 정렬된 첫 페이지 조회
     *  3) cursor가 있으면: 해당 cursor(id)보다 작은 id만 대상으로 다음 페이지 조회
     *  4) Slice를 사용해 hasNext 여부 확인
     *  5) 마지막 matchId를 nextCursor로 사용해 응답에 포함
     *
     * @param hostId 현재 로그인 유저 ID (매치 호스트)
     * @param cursor 마지막으로 조회한 matchId (null이면 첫 페이지)
     * @param size   한 번에 조회할 매치 개수
     */
    public MatchMyListResponseDTO getMyMatches(Long hostId, Long cursor, Integer size) {

        // 1. 페이지 크기 정규화
        int pageSize = normalizeSize(size);

        Pageable pageable = PageRequest.of(0, pageSize);

        // 2. cursor 유무에 따라 다른 쿼리 호출
        // 첫 페이지: hostId 기준으로 id 내림차순 정렬
        // 다음 페이지: cursor보다 작은 id 들만 대상으로 id 내림차순
        Slice<Match> slice;
        if (cursor == null) {
            slice = matchRepository.findByHostIdOrderByIdDesc(hostId, pageable);
        } else {
            slice = matchRepository.findByHostIdAndIdLessThanOrderByIdDesc(hostId, cursor, pageable);
        }

        // 3. Entity → DTO 변환
        List<MatchSearchDTO> content = slice.getContent().stream()
                .map(this::toDto)
                .toList();

        // 4. 다음 페이지 요청 시 사용할 cursor (현재 페이지의 마지막 matchId)
        Long nextCursor = content.isEmpty() ? null : content.get(content.size() - 1).getMatchId();

        // 5. 응답 DTO 구성
        return MatchMyListResponseDTO.builder()
                .matches(content)
                .size(Long.valueOf(content.size()))
                .hasNext(slice.hasNext())
                .cursor(nextCursor)
                .build();
    }

    /**
     * 페이지 크기 정규화
     * - null 이면 DEFAULT_SIZE
     * - 0 이하 이면 예외
     * - MAX_SIZE를 초과하면 MAX_SIZE로 제한
     */
    private int normalizeSize(Integer size) {
        if (size == null) return DEFAULT_SIZE;
        if (size <= 0) throw new IllegalArgumentException("size는 1 이상이어야 합니다.");
        return Math.min(size, MAX_SIZE);
    }

    /**
     * Match 엔티티 → MatchSearchDTO 변환
     *
     * - MatchStatus → OPEN/CLOSED 문자열 매핑
     * - Period, Age VO 컬렉션을 String 리스트로 변환
     * - 날짜/시간은 ISO-8601 문자열로 포맷팅
     */
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