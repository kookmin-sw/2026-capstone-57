package com.ilgiyebo.repository;

import com.ilgiyebo.domain.PlanEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface PlanEntryRepository extends JpaRepository<PlanEntryEntity, UUID> {
    Optional<PlanEntryEntity> findByUserIdAndEntryDate(UUID userId, LocalDate entryDate);
}
