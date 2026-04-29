package com.ilgiyebo.repository;

import com.ilgiyebo.domain.CampusPathEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CampusPathRepository extends JpaRepository<CampusPathEntity, UUID> {
    Optional<CampusPathEntity> findByFromBuildingIdAndToBuildingId(UUID fromBuildingId, UUID toBuildingId);
}
