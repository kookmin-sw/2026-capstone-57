package com.ilgiyebo.domain.campus.repository;

import com.ilgiyebo.domain.campus.entity.PlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlaceRepository extends JpaRepository<PlaceEntity, UUID> {
    List<PlaceEntity> findByBuildingId(UUID buildingId);
    List<PlaceEntity> findByType(String type);
    List<PlaceEntity> findByBuildingIdAndFloor(UUID buildingId, int floor);
}
