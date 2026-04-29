package com.ilgiyebo.repository;

import com.ilgiyebo.domain.CampusVenueEntity;
import com.ilgiyebo.domain.VenueType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface CampusVenueRepository extends JpaRepository<CampusVenueEntity, UUID> {
    List<CampusVenueEntity> findByType(VenueType type);
    List<CampusVenueEntity> findByBuildingId(UUID buildingId);
    List<CampusVenueEntity> findByTypeAndMeetingSuitabilityGreaterThanEqual(VenueType type, int minSuitability);
}
