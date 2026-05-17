package com.ilgiyebo.domain.campus.entity;

import com.ilgiyebo.common.config.TypeActivityListConverter;
import com.ilgiyebo.common.entity.BaseSchema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "campus_building_place")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class CampusBuildingPlaceEntity extends BaseSchema {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private CampusBuildingEntity building;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int floor;

    @Convert(converter = TypeActivityListConverter.class)
    @Column(name = "type_activity", nullable = false, columnDefinition = "TEXT")
    private List<TypeActivity> typeActivity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "operating_hours")
    private String operatingHours;
}
