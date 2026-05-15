package com.ilgiyebo.domain.exp.repository;

import com.ilgiyebo.domain.exp.entity.ExpActivity;
import com.ilgiyebo.domain.exp.entity.ExpHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface ExpHistoryRepository extends JpaRepository<ExpHistoryEntity, UUID> {
    Page<ExpHistoryEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    /**
     * 특정 사용자의 특정 활동에 대해 주어진 시간 이후 지급 횟수를 조회한다.
     * 일일 제한 검증에 사용된다.
     */
    @Query("SELECT COUNT(e) FROM ExpHistoryEntity e " +
           "WHERE e.user.id = :userId AND e.activity = :activity AND e.createdAt >= :since")
    long countByUserIdAndActivitySince(
            @Param("userId") UUID userId,
            @Param("activity") ExpActivity activity,
            @Param("since") LocalDateTime since);
}
