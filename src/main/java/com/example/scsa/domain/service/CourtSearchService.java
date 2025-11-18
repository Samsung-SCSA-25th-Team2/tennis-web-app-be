package com.example.scsa.domain.service;

import com.example.scsa.domain.entity.Court;
import com.example.scsa.dto.CourtDTO;
import com.example.scsa.dto.CourtSearchDTO;
import com.example.scsa.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtSearchService {

    private final CourtRepository courtRepository;

    @Transactional(readOnly = true)
    public CourtSearchDTO searchByKeyword(String keyword){
        List<Court> courts = courtRepository.findByKeyword(keyword);

        List<CourtDTO> items = courts.stream()
                .map(court -> CourtDTO.builder()
                        .courtId(court.getId())
                        .thumbnail(court.getImgUrl())
                        .latitude(court.getLatitude())
                        .longitude(court.getLatitude())
                        .address(court.getLocation())
                        .name(court.getCourtName())
                        .build())
                .collect(Collectors.toList());

        return CourtSearchDTO.builder()
                .courts(items)
                .build();
    }
}
