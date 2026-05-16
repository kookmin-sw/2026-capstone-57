package com.ilgiyebo.repository;

import com.ilgiyebo.domain.DiaryEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface DiaryEntryRepository extends JpaRepository<DiaryEntryEntity, UUID> {
    Page<DiaryEntryEntity> findByUserId(UUID userId, Pageable pageable);
    Optional<DiaryEntryEntity> findByUserIdAndEntryDate(UUID userId, LocalDate entryDate);
}
