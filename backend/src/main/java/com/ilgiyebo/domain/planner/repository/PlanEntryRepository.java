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
     * 특정 사용자가 특정 날짜 범위 내에 특정 source 일정을 작성했는지 확인한다.
     */
    @Query("""
        SELECT COUNT(e) > 0 FROM PlanEntryEntity e
        WHERE e.user.id = :userId
          AND e.source = :source
          AND e.date >= :fromDate
          AND e.date <= :toDate
    """)
    boolean existsEntryBySourceBetween(
            @Param("userId") UUID userId,
            @Param("source") PlanSource source,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    /**
     * 특정 사용자가 특정 날짜에 특정 source 일정 수를 조회한다 (경험치 중복 지급 방지).
     */
    @Query("""
        SELECT COUNT(e) FROM PlanEntryEntity e
        WHERE e.user.id = :userId
          AND e.source = :source
          AND e.date = :date
    """)
    long countEntriesBySourceAndDate(
            @Param("userId") UUID userId,
            @Param("source") PlanSource source,
            @Param("date") LocalDate date);

    /**
     * 특정 사용자의 특정 학기 SCHEDULE_AUTO 일정 중 sourceSchedule이 연결된 것만 삭제한다.
     * SCHEDULE_OVERRIDE는 사용자가 수정한 것이므로 보존한다.
     */
    @Modifying
    @Query("""
        DELETE FROM PlanEntryEntity e
        WHERE e.user.id = :userId
          AND e.sourceSchedule IS NOT NULL
          AND e.source = :source
          AND e.sourceSchedule.semester.id = :semesterId
    """)
    int deleteByUserIdAndSourceScheduleNotNull(
            @Param("userId") UUID userId,
            @Param("source") PlanSource source,
            @Param("semesterId") UUID semesterId);

    /**
     * 특정 사용자의 특정 학기 SCHEDULE_OVERRIDE 일정에서 sourceSchedule FK를 null로 설정한다.
     * schedule 삭제 전 FK 제약 해소용. 일정 자체는 보존한다.
     */
    @Modifying
    @Query("""
        UPDATE PlanEntryEntity e
        SET e.sourceSchedule = NULL
        WHERE e.user.id = :userId
          AND e.source = :source
          AND e.sourceSchedule IS NOT NULL
          AND e.sourceSchedule.semester.id = :semesterId
    """)
    int detachSourceScheduleForOverrides(
            @Param("userId") UUID userId,
            @Param("source") PlanSource source,
            @Param("semesterId") UUID semesterId);

    /**
     * 특정 사용자의 특정 날짜에 MANUAL 또는 SCHEDULE_OVERRIDE 일정 중
     * 주어진 시간 범위와 겹치는 것이 있는지 확인한다.
     * 시간표 자동 생성 시 충돌 검사에 사용한다.
     */
    @Query("""
        SELECT COUNT(e) > 0 FROM PlanEntryEntity e
        WHERE e.user.id = :userId
          AND e.date = :date
          AND e.source IN :sources
          AND e.startTime < :endTime
          AND e.endTime > :startTime
    """)
    boolean existsConflictingUserEntry(
            @Param("userId") UUID userId,
            @Param("date") LocalDate date,
            @Param("sources") List<PlanSource> sources,
            @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);

    /**
     * 특정 사용자의 날짜 범위 내 MANUAL/SCHEDULE_OVERRIDE 일정을 한 번에 조회한다.
     * 주 단위 충돌 검사를 메모리에서 수행하기 위해 사용한다.
     */
    @Query("""
        SELECT e FROM PlanEntryEntity e
        WHERE e.user.id = :userId
          AND e.date >= :fromDate
          AND e.date <= :toDate
          AND e.source IN :sources
        ORDER BY e.date, e.startTime
    """)
    List<PlanEntryEntity> findByUserIdAndDateBetweenAndSourceIn(
            @Param("userId") UUID userId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("sources") List<PlanSource> sources);
}
