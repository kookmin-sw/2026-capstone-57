package com.ilgiyebo.repository;

import com.ilgiyebo.domain.TimetableEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntryEntity, UUID> {
    List<TimetableEntryEntity> findByUserId(UUID userId);
    List<TimetableEntryEntity> findByUserIdAndDayOfWeek(UUID userId, int dayOfWeek);
}
