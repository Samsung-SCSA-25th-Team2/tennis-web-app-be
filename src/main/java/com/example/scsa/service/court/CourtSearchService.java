package com.example.scsa.service.court;

import com.example.scsa.domain.entity.Court;
import com.example.scsa.dto.court.CourtDTO;
import com.example.scsa.dto.court.CourtSearchDTO;
import com.example.scsa.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtSearchService {

    private final CourtRepository courtRepository;

    /**
     * 키워드 기반 테니스장 검색 + 커서 기반 페이징
     *
     * 흐름 요약:
     *  1) cursor가 없으면 처음부터 조회 (cursor=0)
     *  2) size + 1개를 조회해서 다음 페이지가 존재하는지(hasNext) 확인
     *  3) 실제 응답에는 size개만 포함
     *  4) nextCursor = 마지막 courtId
     *  5) 조회 결과(Court 엔티티)를 CourtDTO 리스트로 변환하여 반환
     *
     * 페이징 기준:
     *   - 정렬 기준: id ASC (오래된 → 최신)
     *   - cursor보다 큰 ID만 다음 페이지 대상으로 사용됨
     */
    @Transactional(readOnly = true)
    public CourtSearchDTO searchByKeyword(String keyword, Long cursor, int size) {

        // 1. 커서 기본값 설정
        long effectiveCursor = (cursor == null) ? 0L : cursor;

        /**
         * size + 1개를 조회하여 다음 페이지 여부(hasNext)를 판단하기 위함
         * PageRequest.of(페이지, 크기, 정렬)
         */
        Pageable pageable = PageRequest.of(
                0,
                size + 1,
                Sort.by(Sort.Direction.ASC, "id")
        );

        /**
         * findByKeywordWithCursor():
         *    - keyword가 포함된 테니스장 검색
         *    - id > cursor 조건을 만족하는 레코드만 반환
         *    - Pageable에 따라 size+1개 최대 조회
         */
        List<Court> courts = courtRepository.findByKeywordWithCursor(keyword, effectiveCursor, pageable);

        boolean hasNext = false;

        // 2. size+1개 조회했다면 다음 페이지 존재
        if (courts.size() > size) {
            hasNext = true;
            courts = courts.subList(0, size);
        }

        /**
         * 3. nextCursor 계산
         *
         * - 다음 페이지 요청 시 cursor로 사용됨
         * - courts.isEmpty()인 경우 cursor 1L을 반환
         */
        long nextCursor = courts.isEmpty()
                ? 1L
                : courts.get(courts.size() - 1).getId();

        // 4. Court → CourtDTO 변환
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

        // 5. DTO로 페이징 결과 반환
        return CourtSearchDTO.builder()
                .courts(items)
                .hasNext(hasNext)
                .cursor(nextCursor)
                .size(items.size())
                .build();
    }
}
