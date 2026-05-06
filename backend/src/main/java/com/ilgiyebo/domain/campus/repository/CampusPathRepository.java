package com.ilgiyebo.domain.campus.repository;

import com.ilgiyebo.domain.campus.entity.CampusPathEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CampusPathRepository extends JpaRepository<CampusPathEntity, UUID> {
    List<CampusPathEntity> findByFromBuildingIdAndToBuildingId(UUID fromBuildingId, UUID toBuildingId);
    List<CampusPathEntity> findByFromBuildingId(UUID fromBuildingId);
    List<CampusPathEntity> findByToBuildingId(UUID toBuildingId);
}
