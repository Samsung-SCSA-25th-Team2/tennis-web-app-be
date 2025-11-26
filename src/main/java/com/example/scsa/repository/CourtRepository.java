package com.example.scsa.repository;

import com.example.scsa.domain.entity.Court;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {

    /**
     * 커서 기반 테니스 코트 검색 쿼리
     *
     * - id ASC 정렬 (Pageable의 Sort와 맞춰 사용)
     * - cursor == 0 이면 전체에서 조회
     * - cursor > 0 이면 cursor보다 큰 id만 조회
     *
     * @param keyword 테니스 코트 관련 키워드
     * @param cursor 커서
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    @Query("""
           select c
           from Court c
           where (lower(c.courtName) like lower(concat('%', :keyword, '%'))
                  or lower(c.location)  like lower(concat('%', :keyword, '%')))
             and (:cursor = 0 or c.id > :cursor)
           """)
    List<Court> findByKeywordWithCursor(
            @Param("keyword") String keyword,
            @Param("cursor") long cursor,
            Pageable pageable
    );
}
