package com.example.scsa.service.court;

import com.example.scsa.dto.court.CourtDTO;
import com.example.scsa.domain.entity.Court;
import com.example.scsa.exception.court.CourtNotFoundException;
import com.example.scsa.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;

    /**
     * 테니스장 단건 조회
     *
     * 실행 흐름:
     *  1) courtId로 테니스장을 조회한다.
     *  2) 존재하지 않으면 CourtNotFoundException 발생.
     *  3) Court 엔티티를 CourtDTO로 변환하여 반환.
     *
     */
    @Transactional(readOnly = true)
    public CourtDTO findById(Long courtId) {

        // 1. 테니스장 존재 여부 확인
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new CourtNotFoundException(courtId));

        // 2. DTO 변환 후 반환
        return CourtDTO.builder()
                .courtId(court.getId())
                .thumbnail(court.getImgUrl())
                .latitude(court.getLatitude())
                .longitude(court.getLongitude())
                .address(court.getLocation())
                .name(court.getCourtName())
                .build();
    }
}
