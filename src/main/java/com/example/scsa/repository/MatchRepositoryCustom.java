package com.example.scsa.repository;

import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.vo.GameType;
import com.example.scsa.domain.vo.MatchStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepositoryCustom {

    /**
     * 기본 필터 (날짜/시간, gameType, status)만 적용된 매치 목록 조회.
     * 정렬/거리/추천/커서는 Service에서 처리.
     * 
     * @param from 검색 시작 날짜 
     * @param to 검색 종료 날짜
     * @param gameType 매치 유형
     * @param statuses 매치 모집 상태
     * @return 매치 목록
     */
    List<Match> findMatchesForSearch(LocalDateTime from,
                                     LocalDateTime to,
                                     GameType gameType,
                                     List<MatchStatus> statuses);
}