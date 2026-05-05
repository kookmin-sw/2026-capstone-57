package com.ilgiyebo.repository;

import com.ilgiyebo.domain.BuildingPurpose;
import com.ilgiyebo.domain.CampusBuildingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampusBuildingRepository extends JpaRepository<CampusBuildingEntity, UUID> {
    List<CampusBuildingEntity> findByPurpose(BuildingPurpose purpose);
    Optional<CampusBuildingEntity> findByName(String name);
}
