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

    @Override
    public List<Match> findMatchesForSearch(LocalDateTime from,
                                            LocalDateTime to,
                                            GameType gameType,
                                            List<MatchStatus> statuses) {

        StringBuilder jpql = new StringBuilder(
                "SELECT m FROM Match m " +
                        "JOIN FETCH m.court c " +
                        "JOIN FETCH m.host h " +
                        "WHERE m.matchStartDateTime > :from " +
                        "AND m.matchStartDateTime <= :to"
        );

        if (gameType != null) {
            jpql.append(" AND m.gameType = :gameType");
        }

        if (statuses != null && !statuses.isEmpty()) {
            jpql.append(" AND m.matchStatus IN :statuses");
        }

        TypedQuery<Match> query = em.createQuery(jpql.toString(), Match.class)
                .setParameter("from", from)
                .setParameter("to", to);

        if (gameType != null) {
            query.setParameter("gameType", gameType);
        }
        if (statuses != null && !statuses.isEmpty()) {
            query.setParameter("statuses", statuses);
        }

        return query.getResultList();
    }
}
