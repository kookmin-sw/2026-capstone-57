package com.ilgiyebo.domain.campus.repository;

import com.ilgiyebo.domain.campus.entity.CampusBuildingPlaceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlaceRepository extends JpaRepository<CampusBuildingPlaceEntity, UUID> {
    List<CampusBuildingPlaceEntity> findByBuildingId(UUID buildingId);
    List<CampusBuildingPlaceEntity> findByType(String type);
    List<CampusBuildingPlaceEntity> findByBuildingIdAndFloor(UUID buildingId, int floor);
}
