package com.ilgiyebo.domain.planner.repository;

import com.ilgiyebo.domain.planner.entity.PlanEntryEntity;
import com.ilgiyebo.domain.planner.entity.PlanSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanEntryRepository extends JpaRepository<PlanEntryEntity, UUID> {

    List<PlanEntryEntity> findByUserIdAndDateOrderByStartTimeAsc(UUID userId, LocalDate date);

    List<PlanEntryEntity> findByUserId(UUID userId);

    /**
     * 특정 사용자의 특정 날짜에 시간이 겹치는 일정이 있는지 확인한다.
     * 종료시간=시작시간 맞닿는 경우는 충돌로 보지 않는다.
     * excludeId가 null이 아니면 해당 ID의 일정은 제외한다 (수정 시 자기 자신 제외).
     */
    @Query("""
        SELECT e FROM PlanEntryEntity e
        WHERE e.user.id = :userId
          AND e.date = :date
          AND e.startTime < :endTime
          AND e.endTime > :startTime
          AND (:excludeId IS NULL OR e.id <> :excludeId)
    """)
    List<PlanEntryEntity> findConflicting(
            @Param("userId") UUID userId,
            @Param("date") LocalDate date,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeId") UUID excludeId);

    /**
     * 특정 사용자의 미래 SCHEDULE_AUTO 일정을 삭제한다.
     */
    @Modifying
    @Query("""
        DELETE FROM PlanEntryEntity e
        WHERE e.user.id = :userId
          AND e.source = :source
          AND e.date >= :fromDate
    """)
    int deleteByUserIdAndSourceAndDateAfter(
            @Param("userId") UUID userId,
            @Param("source") PlanSource source,
            @Param("fromDate") LocalDate fromDate);

    /**
     * 특정 사용자가 특정 날짜 범위 내에 source=MANUAL 일정을 작성했는지 확인한다.
     */
    @Query("""
        SELECT COUNT(e) > 0 FROM PlanEntryEntity e
        WHERE e.user.id = :userId
          AND e.source = 'MANUAL'
          AND e.date >= :fromDate
          AND e.date <= :toDate
    """)
    boolean existsManualEntryBetween(
            @Param("userId") UUID userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * 특정 사용자가 특정 날짜에 source=MANUAL 일정을 작성했는지 확인한다 (경험치 중복 지급 방지).
     */
    @Query("""
        SELECT COUNT(e) FROM PlanEntryEntity e
        WHERE e.user.id = :userId
          AND e.source = 'MANUAL'
          AND e.date = :date
    """)
    long countManualEntriesByDate(@Param("userId") UUID userId, @Param("date") LocalDate date);
}
