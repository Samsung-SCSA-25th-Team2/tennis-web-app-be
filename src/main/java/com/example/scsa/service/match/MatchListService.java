package com.example.scsa.service.match;

import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.vo.GameType;
import com.example.scsa.domain.vo.MatchStatus;
import com.example.scsa.dto.match.MatchListRequestDTO;
import com.example.scsa.dto.match.MatchListResponseDTO;
import com.example.scsa.dto.match.MatchSearchDTO;
import com.example.scsa.exception.InvalidMatchQueryParameterException;
import com.example.scsa.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchListService {

    private final MatchRepository matchRepository;
    private final static Integer DEFAULT_SIZE = 10;
    private final static Long DEFAULT_CURSOR = 1L;

    @Transactional(readOnly = true)
    public MatchListResponseDTO getMatchList(MatchListRequestDTO request) {

        // -------------------------
        // 1. sort / size / 위치 파라미터 기본값 + 검증
        // -------------------------

        String sortRaw = request.getSort();
        String sort; // "createdAt" | "latest" | "distance"

        if (sortRaw == null || sortRaw.isBlank()) {
            sort = "createdAt"; // 기본값: Match 생성 시각 기준 desc
        } else if ("latest".equalsIgnoreCase(sortRaw)) {
            sort = "latest";
        } else if ("distance".equalsIgnoreCase(sortRaw)) {
            sort = "distance";
        } else {
            throw new InvalidMatchQueryParameterException("허용되지 않은 sort 값입니다. (latest, distance만 사용 가능)");
        }

        int size = Optional.ofNullable(request.getSize()).orElse(DEFAULT_SIZE);
        if (size <= 0) {
            throw new InvalidMatchQueryParameterException("size는 1 이상이어야 합니다.");
        }


        Double userLat = request.getLatitude();
        Double userLon = request.getLongitude();
        Long radiusKm = request.getRadius();

        // 1-1. latitude / longitude 둘 다 null 또는 둘 다 not null
        if ((userLat == null) != (userLon == null)) { // XOR
            throw new InvalidMatchQueryParameterException("latitude와 longitude는 둘 다 있거나 둘 다 없어야 합니다.");
        }

        // 1-2. radius != null → latitude/longitude 필수
        if (radiusKm != null && (userLat == null || userLon == null)) {
            throw new InvalidMatchQueryParameterException("radius가 존재할 때는 latitude와 longitude가 모두 필요합니다.");
        }

        // 1-3. sort=distance → latitude/longitude 필수
        if ("distance".equals(sort) && (userLat == null || userLon == null)) {
            throw new InvalidMatchQueryParameterException("sort=distance인 경우 latitude와 longitude는 모두 필수입니다.");
        }

        // -------------------------
        // 2. 시간 필터(date, startTime, endTime) 파싱
        // -------------------------

        LocalDateTime startDateTime;
        LocalDateTime endDateTime;

        String dateStr = request.getDate();
        Integer startHour = request.getStartTime() == null ? null : Integer.parseInt(request.getStartTime());
        Integer endHour = request.getEndTime()  == null ? null : Integer.parseInt(request.getEndTime());

        if (startHour != null && (startHour < 0 || startHour > 23)
                || endHour != null && (endHour < 0 || endHour > 23)) {
            throw new InvalidMatchQueryParameterException("startTime과 endTime은 0~23 범위의 정수여야 합니다.");
        }

        if (startHour != null && endHour != null && startHour >= endHour) {
            throw new InvalidMatchQueryParameterException("startTime은 endTime보다 작아야 합니다.");
        }

        if (dateStr != null) {
            LocalDate date;
            try {
                date = LocalDate.parse(dateStr);
            } catch (Exception e) {
                throw new InvalidMatchQueryParameterException("date 형식이 올바르지 않습니다. (예: 2025-11-20)");
            }

            int fromHour = (startHour != null) ? startHour : 0;
            int toHour = (endHour != null) ? endHour : 23;

            startDateTime = date.atTime(fromHour, 0);
            endDateTime = date.atTime(toHour, 0);
        } else {
            // 날짜 조건이 전혀 없으면 전체 범위
            startDateTime = LocalDateTime.of(1970, 1, 1, 0, 0);
            endDateTime = LocalDateTime.of(2100, 12, 31, 23, 59);
        }

        // -------------------------
        // 3. gameType / status / cursor 파싱
        // -------------------------

        GameType gameType = null;
        if (request.getGameType() != null) {
            try {
                gameType = GameType.valueOf(request.getGameType());
            } catch (IllegalArgumentException e) {
                throw new InvalidMatchQueryParameterException("지원하지 않는 gameType 입니다.");
            }
        }

        Set<MatchStatus> statuses = parseStatuses(request.getStatus());

        // cursor: 명세상 default=1 이지만,
        // 실제 페이지네이션에선 null이면 "첫 페이지"로 취급하는 게 자연스러워서
        // null 그대로 넘긴다.
        Long cursorId = request.getCursor();

        // -------------------------
        // 4. 1차 DB 조회 (시간 + gameType + status + cursor)
        // -------------------------

        List<Match> matches = matchRepository.findForList(
                startDateTime,
                endDateTime,
                gameType,
                statuses,
                cursorId
        );

        // -------------------------
        // 5. 거리(radius) 필터 + sort 기준 정렬
        // -------------------------

        if (userLat != null && userLon != null && radiusKm != null) {
            matches = matches.stream()
                    .filter(m -> distanceKm(
                            userLat,
                            userLon,
                            m.getCourt().getLatitude(),
                            m.getCourt().getLongitude()
                    ) <= radiusKm)
                    .collect(Collectors.toList());
        }

        Comparator<Match> comparator = getComparator(sort, userLat, userLon);
        matches.sort(comparator);

        // -------------------------
        // 6. 커서 페이지네이션 (size + 1 → hasNext, nextCursor)
        // -------------------------

        List<Match> limited = matches.stream()
                .limit(size + 1L)
                .collect(Collectors.toList());

        boolean hasNext = limited.size() > size;
        if (hasNext) {
            limited = limited.subList(0, size);
        }

        Long nextCursor = null;
        if (hasNext && !limited.isEmpty()) {
            nextCursor = limited.get(limited.size() - 1).getId();
        }

        // -------------------------
        // 7. DTO 변환
        // -------------------------

        List<MatchSearchDTO> content = limited.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return MatchListResponseDTO.builder()
                .matches(content)
                .size(Long.valueOf(content.size()))
                .hasNext(hasNext)
                .cursor(Optional.ofNullable(nextCursor).orElse(DEFAULT_CURSOR))
                .build();
    }

    // status: "RECRUITING,COMPLETED" → EnumSet
    private Set<MatchStatus> parseStatuses(String statusParam) {
        if (statusParam == null || statusParam.isBlank()) {
            // 기본값: RECRUITING만
            return EnumSet.of(MatchStatus.RECRUITING);
        }

        String[] tokens = statusParam.split(",");
        EnumSet<MatchStatus> set = EnumSet.noneOf(MatchStatus.class);

        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            try {
                set.add(MatchStatus.valueOf(trimmed));
            } catch (IllegalArgumentException e) {
                throw new InvalidMatchQueryParameterException("지원하지 않는 status 값입니다: " + trimmed);
            }
        }

        if (set.isEmpty()) {
            throw new InvalidMatchQueryParameterException("status 파라미터가 올바르지 않습니다.");
        }
        return set;
    }

    // 정렬 기준
    private Comparator<Match> getComparator(String sort, Double lat, Double lon) {
        if ("latest".equals(sort)) {
            // 경기 시작 시간이 빠른 순
            return Comparator.comparing(Match::getMatchStartDateTime)
                    .thenComparing(Match::getId);
        }

        if ("distance".equals(sort)) {
            // 사용자 위치와의 거리 가까운 순
            return Comparator.<Match>comparingDouble(m ->
                            distanceKm(
                                    lat,
                                    lon,
                                    m.getCourt().getLatitude(),
                                    m.getCourt().getLongitude()
                            )
                    )
                    .thenComparing(Match::getMatchStartDateTime)
                    .thenComparing(Match::getId);
        }

        // default: 생성 시간 최신순
        return Comparator.comparing(Match::getCreatedAt)
                .reversed()
                .thenComparing(Match::getId, Comparator.reverseOrder());
    }

    // Haversine formula (간단 버전)
    private double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private MatchSearchDTO toDto(Match match) {
        String statusString = switch (match.getMatchStatus()) {
            case RECRUITING -> "OPEN";
            case COMPLETED -> "CLOSED";
        };

        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        return MatchSearchDTO.builder()
                .matchId(match.getId())
                .hostId(match.getHost().getId())
                .startDateTime(match.getMatchStartDateTime().format(iso))
                .endDateTime(match.getMatchEndDateTime().format(iso))
                .gameType(match.getGameType().name())
                .courtId(match.getCourt().getId())
                // 나머지 필드는 네 엔티티에 맞게 채워도 되고, 일단 null/0으로 둬도 됨
                .period(match.getPeriods().stream().map(p -> p.name()).toList())
                .playerCountMen(match.getPlayerCountMen())
                .playerCountWomen(match.getPlayerCountWomen())
                .ageRange(match.getAges().stream().map(a -> a.name()).toList())
                .status(statusString)
                .createdAt(match.getCreatedAt().format(iso))
                .fee(match.getFee())
                .build();
    }
}
