package com.ilgiyebo.domain.campus.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
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

    @OneToMany(mappedBy = "path", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    @Builder.Default
    private List<CampusPathVenueEntity> pathVenues = new ArrayList<>();
}
