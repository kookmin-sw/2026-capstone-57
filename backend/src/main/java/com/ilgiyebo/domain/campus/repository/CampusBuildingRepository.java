package com.ilgiyebo.domain.campus.repository;

import com.ilgiyebo.domain.campus.entity.CampusBuildingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CampusBuildingRepository extends JpaRepository<CampusBuildingEntity, UUID> {
    Optional<CampusBuildingEntity> findByName(String name);
}
