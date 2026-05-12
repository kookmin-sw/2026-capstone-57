package com.ilgiyebo.domain.user.repository;

import com.ilgiyebo.domain.user.entity.SemesterEntity;
import com.ilgiyebo.domain.user.entity.SemesterTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface SemesterRepository extends JpaRepository<SemesterEntity, UUID> {

    Optional<SemesterEntity> findByYearAndTerm(int year, SemesterTerm term);

    /**
     * 주어진 날짜가 포함되는 활성 학기를 조회한다.
     */
    @Query("""
        SELECT s FROM SemesterEntity s
        WHERE s.startedAt <= :date AND s.endedAt >= :date
    """)
    Optional<SemesterEntity> findCurrentByDate(@Param("date") LocalDate date);
}
