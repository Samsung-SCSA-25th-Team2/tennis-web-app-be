package com.example.scsa.service.match;

import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.vo.GameType;
import com.example.scsa.domain.vo.MatchStatus;
import com.example.scsa.dto.match.MatchListRequestDTO;
import com.example.scsa.dto.match.MatchListResponseDTO;
import com.example.scsa.dto.match.MatchSearchDTO;
import com.example.scsa.exception.match.InvalidMatchSearchParameterException;
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

    // 위치 정보가 없을 때 사용할 기본 좌표 (서울 시청 근처)
    private static final double DEFAULT_LAT = 37.5666;
    private static final double DEFAULT_LNG = 126.9782;

    // ISO-8601 포맷(응답/커서에서 공통 사용)
    // Z 없이 응답 (프론트엔드 규약 변경: 2025-11-17T19:00:00)
    private static final DateTimeFormatter ISO_DATETIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;

    /**
     * 매치 목록 조회 메인 서비스
     *
     * 전체 흐름:
     *  1) 요청 파라미터 기본값 세팅 + 유효성 검사 (날짜/시간/정렬/size 등)
     *  2) gameType, status 파싱
     *  3) 위치/반경 정보 처리 (sort=distance일 때 좌표 필수)
     *  4) 기본 조건(from~to, gameType, status)에 맞는 매치 목록 DB 조회
     *  5) 각 매치에 대해 거리(distanceKm), 추천 점수(score) 계산
     *  6) 반경(radius) 필터 적용
     *  7) 정렬 기준(sort)에 맞게 정렬
     *  8) cursor가 있다면 메모리 상에서 cursor 이후 데이터만 남김
     *  9) size 기준으로 페이징 + hasNext 판단
     * 10) 마지막 요소 기준 nextCursor 생성(Base64 인코딩)
     * 11) Match 엔티티 + 계산값 → MatchSearchDTO로 매핑 후 응답 생성
     */
    public MatchListResponseDTO getMatchList(MatchListRequestDTO request) {

        // 1) 기본값 & Validation
        LocalDateTime now = LocalDateTime.now();

        // 정렬 기준 기본값: createdAt
        String sort = (request.getSort() == null || request.getSort().isBlank())
                ? "createdAt"
                : request.getSort();

        int size = (request.getSize() == null || request.getSize() <= 0)
                ? 10
                : request.getSize();

        // 날짜 기본값: startDate = 오늘, endDate = 9998-12-31
        LocalDate startDate = (request.getStartDate() != null)
                ? LocalDate.parse(request.getStartDate())
                : now.toLocalDate();

        LocalDate endDate = (request.getEndDate() != null)
                ? LocalDate.parse(request.getEndDate())
                : LocalDate.of(9998, 12, 31);

        if (startDate.isAfter(endDate)) {
            throw new InvalidMatchSearchParameterException("startDate는 endDate보다 이후일 수 없습니다.");
        }

        int startHour = request.getStartTime() != null ? request.getStartTime() : 0;
        int endHour = request.getEndTime() != null ? request.getEndTime() : 24;

        // 시간 범위 유효성 검사
        if (startHour < 0 || startHour > 23 || endHour < 1 || endHour > 24 || startHour >= endHour) {
            throw new InvalidMatchSearchParameterException("잘못된 시간 범위입니다.");
        }

        LocalDateTime from = startDate.atTime(startHour, 0);
        LocalDateTime to = endDate.atTime(endHour == 24 ? 23 : endHour, endHour == 24 ? 59 : 0);

        // 항상 “현재 시각 이후” 매치만 조회
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

        // 3) 위치/반경 처리 (sort=distance일 때 좌표 필수)
        double[] latLng = resolveLatLng(sort, request.getLatitude(), request.getLongitude());
        double lat = latLng[0];
        double lng = latLng[1];

        // 반경 기본값: 25km
        int radius = (request.getRadius() == null || request.getRadius() <= 0)
                ? 25
                : request.getRadius();

        // 4) 기본 필터된 매치 목록 조회
        List<Match> matches = matchRepository.findMatchesForSearch(from, to, gameType, statuses);

        // 5) 각 매치에 대해 거리/추천점수 같이 들고 다니기 위한 래퍼로 감싸기
        List<MatchWithMetrics> withMetrics = matches.stream()
                .map(m -> {
                    double distanceKm = calculateDistanceKm(
                            lat, lng,
                            m.getCourt().getLatitude(),
                            m.getCourt().getLongitude()
                    );
                    double score = calculateScore(sort, now, distanceKm, m.getMatchStartDateTime());
                    return new MatchWithMetrics(m, distanceKm, score);
                })
                .collect(Collectors.toList());

        // 6) radius(반경) 필터: 기준 좌표에서 radius km 이내만 남김
        withMetrics = withMetrics.stream()
                .filter(w -> w.distanceKm <= radius)
                .collect(Collectors.toList());

        // 7) 정렬: createdAt / latest / distance / recommend 에 따른 정렬
        sortMatches(withMetrics, sort);

        // 8) cursor 적용 (메모리 상에서 필터링)
        //    - 이전 요청에서 넘겨준 cursor 이후의 데이터만 남김
        if (request.getCursor() != null && !request.getCursor().isBlank()) {
            withMetrics = applyCursorFilter(withMetrics, sort, request.getCursor());
        }

        // 9) 페이징 처리: size + hasNext
        boolean hasNext = withMetrics.size() > size;

        List<MatchWithMetrics> page = withMetrics.stream()
                .limit(size)
                .collect(Collectors.toList());

        // 10) nextCursor 생성: 페이지의 마지막 요소 기준으로 생성
        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            MatchWithMetrics last = page.get(page.size() - 1);
            nextCursor = encodeCursor(sort, last);
        }

        // 11) 응답 DTO 매핑
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

    /**
     * status 쿼리 파라미터 파싱
     * - null/빈값 → 기본값 "RECRUITING"
     * - 콤마(,) 기준으로 여러 개 허용: "RECRUITING,COMPLETED"
     */
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

    /**
     * 위도/경도 처리 로직
     *
     * - sort=distance 일 때는 lat/lng 필수
     * - 그 외 sort 값에서는
     *      • 둘 다 null이면 DEFAULT_LAT/LNG 사용
     *      • 하나만 null이면 예외
     */
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
            // 위치 정보가 전혀 없으면 기본 좌표 사용
            return new double[]{DEFAULT_LAT, DEFAULT_LNG};
        }

        if (lat == null || lng == null) {
            throw new InvalidMatchSearchParameterException("latitude와 longitude는 둘 다 존재하거나 둘 다 null이어야 합니다.");
        }

        return new double[]{lat, lng};
    }

    /**
     * Haversine 공식으로 두 좌표 간 거리(km) 계산
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
     * recommend 정렬일 때 사용할 점수 계산
     *
     * - sort != recommend 이면 0 리턴
     * - timeScore: 현재 시각과 매치 시작 시각의 차이(분)를 최대 1일(1440분) 기준으로 0~1로 정규화
     * - distanceScore: 거리(km)를 반경 25km 기준으로 0~1로 정규화
     * - 최종점수 = 0.7 * timeScore + 0.3 * distanceScore
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

    /**
     * 정렬 기준별 Comparator 적용
     *
     * - latest     : 매치 시작일시 오름차순
     * - distance   : 거리 오름차순
     * - recommend  : score 오름차순
     * - createdAt  : 생성일시 오름차순 (기본값)
     *
     * 동일 값일 때는 match.id로 2차 정렬
     */
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
     * cursor 이후 데이터만 남기는 필터 (메모리 상에서 처리)
     *
     * cursorBase64:
     *  - sort별로 다른 구조의 JSON을 Base64로 인코딩한 문자열
     *      * latest    : { startDateTime, id }
     *      * distance  : { distance, id }
     *      * recommend : { score, id }
     *      * createdAt : { createdAt, id }
     *
     *  - 디코딩해서 cursor 기준을 복원한 뒤,
     *    "cursor보다 이후"인 데이터부터 서브리스트를 반환
     */
    private List<MatchWithMetrics> applyCursorFilter(List<MatchWithMetrics> list,
                                                     String sort,
                                                     String cursorBase64) {

        try {
            // Base64 → JSON String
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
     *
     * 정렬 기준에 따라 서로 다른 Cursor DTO를 만들어 JSON 직렬화 후
     * Base64 인코딩해서 문자열로 응답에 넣는다.
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
     * Entity + 부가 정보(MatchWithMetrics) → 응답 DTO 매핑
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
                // VO 컬렉션들을 문자열 리스트로 변환 (예: ["ONE_YEAR", "TWO_YEARS"])
                .period(m.getPeriods().stream().map(Enum::name).collect(Collectors.toList()))
                .playerCountMen(m.getPlayerCountMen())
                .playerCountWomen(m.getPlayerCountWomen())
                .ageRange(m.getAges().stream().map(Enum::name).collect(Collectors.toList()))
                .status(mapStatus(m.getMatchStatus()))
                .createdAt(m.getCreatedAt().format(ISO_DATETIME))
                .build();
    }

    /**
     * 내부 도메인 MatchStatus → API 응답용 문자열 매핑
     * (예: RECRUITING → "OPEN", COMPLETED → "CLOSED")
     */
    private String mapStatus(MatchStatus status) {
        // 명세: 응답 status는 OPEN/CLOSED
        return switch (status) {
            case RECRUITING -> "OPEN";
            case COMPLETED -> "CLOSED";
        };
    }

    // ==========================
    // 내부용 래퍼 & 커서 DTO
    // ==========================

    /**
     * Match + 거리/추천 점수 정보를 묶어서 다루기 위한 내부용 래퍼 클래스
     */
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

    /**
     * createdAt 정렬용 커서 구조
     */
    private static class CreatedAtCursor {
        public String createdAt;
        public Long id;

        public CreatedAtCursor() { }

        public CreatedAtCursor(String createdAt, Long id) {
            this.createdAt = createdAt;
            this.id = id;
        }
    }

    /**
     * latest(매치 시작 시간) 정렬용 커서 구조
     */
    private static class LatestCursor {
        public String startDateTime;
        public Long id;

        public LatestCursor() { }

        public LatestCursor(String startDateTime, Long id) {
            this.startDateTime = startDateTime;
            this.id = id;
        }
    }

    /**
     * distance 정렬용 커서 구조
     */
    private static class DistanceCursor {
        public double distance;
        public Long id;

        public DistanceCursor() { }

        public DistanceCursor(double distance, Long id) {
            this.distance = distance;
            this.id = id;
        }
    }

    /**
     * recommend 정렬용 커서 구조
     */
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
