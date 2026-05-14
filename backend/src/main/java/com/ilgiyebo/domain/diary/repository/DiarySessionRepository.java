package com.ilgiyebo.domain.diary.repository;

import com.ilgiyebo.domain.diary.entity.DiarySessionEntity;
import com.ilgiyebo.domain.diary.entity.DiarySessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DiarySessionRepository extends JpaRepository<DiarySessionEntity, UUID> {

    /**
     * 사용자별 날짜별 특정 상태의 세션 조회
     */
    Optional<DiarySessionEntity> findByUserIdAndTargetDateAndStatus(
            UUID userId, LocalDate targetDate, DiarySessionStatus status);

    /**
     * 사용자별 날짜별 특정 상태 목록에 해당하는 세션 존재 여부
     */
    boolean existsByUserIdAndTargetDateAndStatusIn(
            UUID userId, LocalDate targetDate, List<DiarySessionStatus> statuses);

    /**
     * 사용자별 진행 중인 세션 조회 (IN_PROGRESS 또는 READY_TO_GENERATE 또는 GENERATED)
     */
    Optional<DiarySessionEntity> findByUserIdAndTargetDateAndStatusIn(
            UUID userId, LocalDate targetDate, List<DiarySessionStatus> statuses);
}
