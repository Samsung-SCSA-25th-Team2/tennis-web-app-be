package com.example.scsa.repository;

import com.example.scsa.domain.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@Repository
public interface CourtRepository extends JpaRepository<Court, Long> {

    @Query("""
           select c
           from Court c
           where lower(c.courtName) like lower(concat('%', :keyword, '%'))
              or lower(c.location)  like lower(concat('%', :keyword, '%'))
           """)
    List<Court> findByKeyword (@Param("keyword") String keyword);
}
