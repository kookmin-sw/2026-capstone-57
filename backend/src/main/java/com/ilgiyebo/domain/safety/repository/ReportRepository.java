package com.ilgiyebo.repository;

import com.ilgiyebo.domain.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<ReportEntity, UUID> {
    long countByTargetId(UUID targetId);
}
