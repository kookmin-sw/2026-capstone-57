package com.ilgiyebo.domain.planner.repository;

import com.ilgiyebo.domain.planner.entity.PlanEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PlanEntryRepository extends JpaRepository<PlanEntryEntity, UUID> {
    List<PlanEntryEntity> findByUserId(UUID userId);
    List<PlanEntryEntity> findByUserIdAndDayOfWeek(UUID userId, int dayOfWeek);
}
