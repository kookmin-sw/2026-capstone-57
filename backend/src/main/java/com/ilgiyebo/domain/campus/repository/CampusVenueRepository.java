package com.ilgiyebo.domain.campus.repository;

import com.ilgiyebo.domain.campus.entity.CampusVenueEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CampusVenueRepository extends JpaRepository<CampusVenueEntity, UUID> {
    Optional<CampusVenueEntity> findByName(String name);
}
