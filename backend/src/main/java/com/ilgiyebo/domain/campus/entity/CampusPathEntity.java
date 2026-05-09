package com.ilgiyebo.domain.campus.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "campus_path")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class CampusPathEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_building_id", nullable = false)
    private CampusBuildingEntity fromBuilding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_building_id", nullable = false)
    private CampusBuildingEntity toBuilding;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id", nullable = false)
    private CampusVenueEntity venue;
}
