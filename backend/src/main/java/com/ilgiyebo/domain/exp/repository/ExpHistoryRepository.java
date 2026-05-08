package com.ilgiyebo.domain.exp.repository;

import com.ilgiyebo.domain.exp.entity.ExpHistoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ExpHistoryRepository extends JpaRepository<ExpHistoryEntity, UUID> {
    Page<ExpHistoryEntity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
