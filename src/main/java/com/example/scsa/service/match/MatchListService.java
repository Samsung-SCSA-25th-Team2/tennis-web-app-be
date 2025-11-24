package com.example.scsa.service.match;

import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.vo.GameType;
import com.example.scsa.domain.vo.MatchStatus;
import com.example.scsa.dto.match.MatchListRequestDTO;
import com.example.scsa.dto.match.MatchListResponseDTO;
import com.example.scsa.dto.match.MatchSearchDTO;
import com.example.scsa.exception.InvalidMatchSearchParameterException;
import com.example.scsa.repository.MatchRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchListService {

    private static final double DEFAULT_LAT = 37.5666;   // 서울시청
    private static final double DEFAULT_LNG = 126.9782;

    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;

    public MatchListResponseDTO getMatchList(MatchListRequestDTO request) {

        // 1) 기본값 & Validation
        LocalDateTime now = LocalDateTime.now();

        String sort = (request.getSort() == null || request.getSort().isBlank())
                ? "createdAt"
                : request.getSort();

        int size = (request.getSize() == null || request.getSize() <= 0)
                ? 10
                : request.getSize();

        LocalDate startDate = (request.getStartDate() != null)
                ? LocalDate.parse(request.getStartDate())
                : now.toLocalDate();

        LocalDate endDate = (request.getEndDate() != null)
                ? LocalDate.parse(request.getEndDate())
                : LocalDate.of(9999, 12, 31);

        if (startDate.isAfter(endDate)) {
            throw new InvalidMatchSearchParameterException("startDate는 endDate보다 이후일 수 없습니다.");
        }

        int startHour = request.getStartTime() != null ? request.getStartTime() : 0;
        int endHour = request.getEndTime() != null ? request.getEndTime() : 24;

        if (startHour < 0 || startHour > 23 || endHour < 1 || endHour > 24 || startHour >= endHour) {
            throw new InvalidMatchSearchParameterException("잘못된 시간 범위입니다.");
        }

        LocalDateTime from = startDate.atTime(startHour, 0);
        LocalDateTime to = endDate.atTime(endHour == 24 ? 23 : endHour, endHour == 24 ? 59 : 0);

        // 항상 현재 시각 이후만 조회
        if (from.isBefore(now)) {
            from = now;
        }

        // 2) gameType, status 파싱
        GameType gameType = null;
        if (request.getGameType() != null && !request.getGameType().isBlank()) {
            try {
                gameType = GameType.valueOf(request.getGameType());
            } catch (IllegalArgumentException e) {
                throw new InvalidMatchSearchParameterException("존재하지 않는 gameType 입니다.");
            }
        }

        List<MatchStatus> statuses = parseStatus(request.getStatus());

        // 3) 위치/반경 처리
        double[] latLng = resolveLatLng(sort, request.getLatitude(), request.getLongitude());
        double lat = latLng[0];
        double lng = latLng[1];

        int radius = (request.getRadius() == null || request.getRadius() <= 0)
                ? 25
                : request.getRadius();

        // 4) 기본 필터된 매치 목록 조회
        List<Match> matches = matchRepository.findMatchesForSearch(from, to, gameType, statuses);

        // 5) 거리, Score 계산
        List<MatchWithMetrics> withMetrics = matches.stream()
                .map(m -> {
                    double distanceKm = calculateDistanceKm(lat, lng,
                            m.getCourt().getLatitude(),
                            m.getCourt().getLongitude());
                    double score = calculateScore(sort, now, distanceKm, m.getMatchStartDateTime());
                    return new MatchWithMetrics(m, distanceKm, score);
                })
                .collect(Collectors.toList());

        // 6) radius 필터 적용
        withMetrics = withMetrics.stream()
                .filter(w -> w.distanceKm <= radius)
                .collect(Collectors.toList());

        // 7) 정렬
        sortMatches(withMetrics, sort);

        // 8) cursor 적용 (메모리에서 filtering)
        if (request.getCursor() != null && !request.getCursor().isBlank()) {
            withMetrics = applyCursorFilter(withMetrics, sort, request.getCursor());
        }

        // 9) 페이징 (size + hasNext)
        boolean hasNext = withMetrics.size() > size;

        List<MatchWithMetrics> page = withMetrics.stream()
                .limit(size)
                .collect(Collectors.toList());

        // 10) nextCursor 생성
        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            MatchWithMetrics last = page.get(page.size() - 1);
            nextCursor = encodeCursor(sort, last);
        }

        // 11) DTO 매핑
        List<MatchSearchDTO> content = page.stream()
                .map(this::toSearchDTO)
                .collect(Collectors.toList());

        return MatchListResponseDTO.builder()
                .matches(content)
                .size(Long.valueOf(content.size()))
                .hasNext(hasNext)
                .cursor(nextCursor)
                .build();
    }

    // ==========================
    // Helper methods
    // ==========================

    private List<MatchStatus> parseStatus(String statusParam) {
        String value = (statusParam == null || statusParam.isBlank())
                ? "RECRUITING"
                : statusParam;

        String[] tokens = value.split(",");
        List<MatchStatus> result = new ArrayList<>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) continue;
            try {
                result.add(MatchStatus.valueOf(trimmed));
            } catch (IllegalArgumentException e) {
                throw new InvalidMatchSearchParameterException("status 값이 잘못되었습니다: " + trimmed);
            }
        }
        return result;
    }

    private double[] resolveLatLng(String sort, Double lat, Double lng) {
        // sort=distance → lat/lng 필수
        if ("distance".equals(sort)) {
            if (lat == null || lng == null) {
                throw new InvalidMatchSearchParameterException("sort=distance일 때 latitude와 longitude는 필수입니다.");
            }
            return new double[]{lat, lng};
        }

        // recommend/createdAt/latest
        if (lat == null && lng == null) {
            return new double[]{DEFAULT_LAT, DEFAULT_LNG};
        }

        if (lat == null || lng == null) {
            throw new InvalidMatchSearchParameterException("latitude와 longitude는 둘 다 존재하거나 둘 다 null이어야 합니다.");
        }

        return new double[]{lat, lng};
    }

    /**
     * Haversine 거리 계산 (km)
     */
    private double calculateDistanceKm(double lat1, double lng1, double lat2, double lng2) {
        final int R = 6371; // 지구 반지름(km)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * recommend일 때만 Score 계산, 그 외는 0
     */
    private double calculateScore(String sort, LocalDateTime now, double distanceKm, LocalDateTime startDateTime) {
        if (!"recommend".equals(sort)) {
            return 0.0;
        }

        long minutesUntil = ChronoUnit.MINUTES.between(now, startDateTime);
        if (minutesUntil < 0) minutesUntil = 0;

        double timeScore = Math.min((double) minutesUntil / 1440.0, 1.0);
        double distanceScore = Math.min(distanceKm / 25.0, 1.0);

        return 0.7 * timeScore + 0.3 * distanceScore;
    }

    private void sortMatches(List<MatchWithMetrics> list, String sort) {
        switch (sort) {
            case "latest":
                list.sort(Comparator
                        .comparing((MatchWithMetrics m) -> m.match.getMatchStartDateTime())
                        .thenComparing(m -> m.match.getId()));
                break;
            case "distance":
                list.sort(Comparator
                        .comparingDouble((MatchWithMetrics m) -> m.distanceKm)
                        .thenComparing(m -> m.match.getId()));
                break;
            case "recommend":
                list.sort(Comparator
                        .comparingDouble((MatchWithMetrics m) -> m.score)
                        .thenComparing(m -> m.match.getId()));
                break;
            case "createdAt":
            default:
                list.sort(Comparator
                        .comparing((MatchWithMetrics m) -> m.match.getCreatedAt())
                        .thenComparing(m -> m.match.getId()));
        }
    }

    /**
     * cursor 이후 데이터만 남기는 필터 (메모리 상)
     */
    private List<MatchWithMetrics> applyCursorFilter(List<MatchWithMetrics> list,
                                                     String sort,
                                                     String cursorBase64) {

        try {
            String json = new String(Base64.getDecoder().decode(cursorBase64));

            int startIndex = 0;

            switch (sort) {
                case "latest": {
                    LatestCursor c = objectMapper.readValue(json, LatestCursor.class);
                    LocalDateTime cursorStart = LocalDateTime.parse(c.startDateTime);
                    Long cursorId = c.id;

                    for (int i = 0; i < list.size(); i++) {
                        MatchWithMetrics m = list.get(i);
                        LocalDateTime start = m.match.getMatchStartDateTime();
                        if (start.isAfter(cursorStart)
                                || (start.equals(cursorStart) && m.match.getId() > cursorId)) {
                            startIndex = i;
                            break;
                        }
                    }
                    break;
                }
                case "distance": {
                    DistanceCursor c = objectMapper.readValue(json, DistanceCursor.class);
                    double cursorDist = c.distance;
                    long cursorId = c.id;

                    for (int i = 0; i < list.size(); i++) {
                        MatchWithMetrics m = list.get(i);
                        if (m.distanceKm > cursorDist
                                || (Double.compare(m.distanceKm, cursorDist) == 0 && m.match.getId() > cursorId)) {
                            startIndex = i;
                            break;
                        }
                    }
                    break;
                }
                case "recommend": {
                    RecommendCursor c = objectMapper.readValue(json, RecommendCursor.class);
                    double cursorScore = c.score;
                    long cursorId = c.id;

                    for (int i = 0; i < list.size(); i++) {
                        MatchWithMetrics m = list.get(i);
                        if (m.score > cursorScore
                                || (Double.compare(m.score, cursorScore) == 0 && m.match.getId() > cursorId)) {
                            startIndex = i;
                            break;
                        }
                    }
                    break;
                }
                case "createdAt":
                default: {
                    CreatedAtCursor c = objectMapper.readValue(json, CreatedAtCursor.class);
                    LocalDateTime cursorCreatedAt = LocalDateTime.parse(c.createdAt);
                    long cursorId = c.id;

                    for (int i = 0; i < list.size(); i++) {
                        MatchWithMetrics m = list.get(i);
                        LocalDateTime created = m.match.getCreatedAt();
                        if (created.isAfter(cursorCreatedAt)
                                || (created.equals(cursorCreatedAt) && m.match.getId() > cursorId)) {
                            startIndex = i;
                            break;
                        }
                    }
                }
            }

            if (startIndex <= 0) {
                return list;
            }
            return list.subList(startIndex, list.size());

        } catch (Exception e) {
            throw new InvalidMatchSearchParameterException("잘못된 cursor 값입니다.", e);
        }
    }

    /**
     * 마지막 요소 기준으로 cursor JSON → Base64 생성
     */
    private String encodeCursor(String sort, MatchWithMetrics last) {
        try {
            String json;

            switch (sort) {
                case "latest": {
                    LatestCursor c = new LatestCursor(
                            last.match.getMatchStartDateTime().format(ISO_DATETIME),
                            last.match.getId()
                    );
                    json = objectMapper.writeValueAsString(c);
                    break;
                }
                case "distance": {
                    DistanceCursor c = new DistanceCursor(last.distanceKm, last.match.getId());
                    json = objectMapper.writeValueAsString(c);
                    break;
                }
                case "recommend": {
                    RecommendCursor c = new RecommendCursor(last.score, last.match.getId());
                    json = objectMapper.writeValueAsString(c);
                    break;
                }
                case "createdAt":
                default: {
                    CreatedAtCursor c = new CreatedAtCursor(
                            last.match.getCreatedAt().format(ISO_DATETIME),
                            last.match.getId()
                    );
                    json = objectMapper.writeValueAsString(c);
                }
            }

            return Base64.getEncoder().encodeToString(json.getBytes());

        } catch (Exception e) {
            throw new IllegalStateException("cursor 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Entity → Response DTO 매핑
     */
    private MatchSearchDTO toSearchDTO(MatchWithMetrics w) {
        Match m = w.match;

        return MatchSearchDTO.builder()
                .matchId(m.getId())
                .hostId(m.getHost().getId())
                .startDateTime(m.getMatchStartDateTime().format(ISO_DATETIME))
                .endDateTime(m.getMatchEndDateTime().format(ISO_DATETIME))
                .gameType(m.getGameType().name())
                .courtId(m.getCourt().getId())
                .fee(m.getFee())
                // 아래 두 줄은 실제 엔티티 구조에 맞게 수정
                .period(m.getPeriods().stream().map(Enum::name).collect(Collectors.toList()))
                .playerCountMen(m.getPlayerCountMen())
                .playerCountWomen(m.getPlayerCountWomen())
                .ageRange(m.getAges().stream().map(Enum::name).collect(Collectors.toList()))
                .status(mapStatus(m.getMatchStatus()))
                .createdAt(m.getCreatedAt().format(ISO_DATETIME))
                .build();
    }

    private String mapStatus(MatchStatus status) {
        // 명세: 응답 status는 OPEN/CLOSED로 내려가는 예시
        return switch (status) {
            case RECRUITING -> "OPEN";
            case COMPLETED -> "CLOSED";
        };
    }

    // ==========================
    // 내부용 래퍼 & 커서 DTO
    // ==========================

    private static class MatchWithMetrics {
        private final Match match;
        private final double distanceKm;
        private final double score;

        private MatchWithMetrics(Match match, double distanceKm, double score) {
            this.match = match;
            this.distanceKm = distanceKm;
            this.score = score;
        }
    }

    private static class CreatedAtCursor {
        public String createdAt;
        public Long id;

        public CreatedAtCursor() { }

        public CreatedAtCursor(String createdAt, Long id) {
            this.createdAt = createdAt;
            this.id = id;
        }
    }

    private static class LatestCursor {
        public String startDateTime;
        public Long id;

        public LatestCursor() { }

        public LatestCursor(String startDateTime, Long id) {
            this.startDateTime = startDateTime;
            this.id = id;
        }
    }

    private static class DistanceCursor {
        public double distance;
        public Long id;

        public DistanceCursor() { }

        public DistanceCursor(double distance, Long id) {
            this.distance = distance;
            this.id = id;
        }
    }

    private static class RecommendCursor {
        public double score;
        public Long id;

        public RecommendCursor() {
        }

        public RecommendCursor(double score, Long id) {
            this.score = score;
            this.id = id;
        }
    }
}
