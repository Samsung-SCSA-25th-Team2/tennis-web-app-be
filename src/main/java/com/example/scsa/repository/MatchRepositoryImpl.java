package com.example.scsa.repository;

import com.example.scsa.domain.entity.Match;
import com.example.scsa.domain.vo.GameType;
import com.example.scsa.domain.vo.MatchStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class MatchRepositoryImpl implements MatchRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    /**
     * 매치 검색용 JPQL 동적 쿼리
     *
     * 필터링 조건:
     *  - 진행 시간(from < start <= to)
     *  - gameType (선택)
     *  - matchStatus 리스트 (선택)
     *
     * JOIN FETCH:
     *  - court, host 를 함께 로딩해 N+1 방지
     *
     * @return 조건에 맞는 매치 리스트
     */
    @Override
    public List<Match> findMatchesForSearch(LocalDateTime from,
                                            LocalDateTime to,
                                            GameType gameType,
                                            List<MatchStatus> statuses) {

        // 기본 검색 조건
        StringBuilder jpql = new StringBuilder(
                "SELECT m FROM Match m " +
                        "JOIN FETCH m.court c " +
                        "JOIN FETCH m.host h " +
                        "WHERE m.matchStartDateTime > :from " +
                        "AND m.matchStartDateTime <= :to"
        );

        // 선택 조건(gameType)
        if (gameType != null) {
            jpql.append(" AND m.gameType = :gameType");
        }

        // 선택 조건(status 리스트)
        if (statuses != null && !statuses.isEmpty()) {
            jpql.append(" AND m.matchStatus IN :statuses");
        }

        // 쿼리 생성 및 기본 파라미터 설정
        TypedQuery<Match> query = em.createQuery(jpql.toString(), Match.class)
                .setParameter("from", from)
                .setParameter("to", to);

        // 선택 파라미터 설정
        if (gameType != null) {
            query.setParameter("gameType", gameType);
        }
        if (statuses != null && !statuses.isEmpty()) {
            query.setParameter("statuses", statuses);
        }

        return query.getResultList();
    }
}