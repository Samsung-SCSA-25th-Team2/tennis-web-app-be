package com.example.scsa.service.court;

import com.example.scsa.domain.entity.Court;
import com.example.scsa.dto.court.CourtDTO;
import com.example.scsa.dto.court.CourtSearchDTO;
import com.example.scsa.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtSearchService {

    private final CourtRepository courtRepository;

    @Transactional(readOnly = true)
    public CourtSearchDTO searchByKeyword(String keyword, Long cursor, int size) {

        long effectiveCursor = (cursor == null) ? 0L : cursor;

        // size + 1개를 조회해서 hasNext 여부 판단
        Pageable pageable = PageRequest.of(
                0,
                size + 1,
                Sort.by(Sort.Direction.ASC, "id")
        );

        List<Court> courts = courtRepository.findByKeywordWithCursor(keyword, effectiveCursor, pageable);

        boolean hasNext = false;

        if (courts.size() > size) {
            hasNext = true;
            courts = courts.subList(0, size);
        }

        long nextCursor = courts.isEmpty()
                ? 1L
                : courts.get(courts.size() - 1).getId();

        List<CourtDTO> items = courts.stream()
                .map(court -> CourtDTO.builder()
                        .courtId(court.getId())
                        .thumbnail(court.getImgUrl())
                        .latitude(court.getLatitude())
                        .longitude(court.getLongitude())
                        .address(court.getLocation())
                        .name(court.getCourtName())
                        .build())
                .collect(Collectors.toList());

        return CourtSearchDTO.builder()
                .courts(items)
                .hasNext(hasNext)
                .cursor(nextCursor)          // 다음 요청 시 사용할 마지막 courtId (없으면 0)
                .size(items.size())          // 현재 페이지 내 데이터 개수
                .build();
    }
}
