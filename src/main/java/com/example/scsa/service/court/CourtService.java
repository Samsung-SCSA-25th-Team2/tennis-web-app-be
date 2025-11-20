package com.example.scsa.service.court;

import com.example.scsa.dto.court.CourtDTO;
import com.example.scsa.domain.entity.Court;
import com.example.scsa.exception.CourtNotFoundException;
import com.example.scsa.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;

    @Transactional(readOnly = true)
    public CourtDTO findById(Long courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new CourtNotFoundException(courtId));

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
