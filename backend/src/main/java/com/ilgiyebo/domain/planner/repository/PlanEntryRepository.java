package com.ilgiyebo.domain.planner.repository;

import com.ilgiyebo.domain.planner.entity.PlanEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PlanEntryRepository extends JpaRepository<PlanEntryEntity, UUID> {
    List<PlanEntryEntity> findByUser_Id(UUID userId);
    List<PlanEntryEntity> findByUser_IdAndDayOfWeek(UUID userId, int dayOfWeek);
}
