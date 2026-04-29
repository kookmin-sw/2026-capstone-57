package com.ilgiyebo.domain;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.config.JsonStringListConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "CAMPUS_PATH")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class CampusPathEntity extends BaseSchema {

    @Column(name = "from_building_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID fromBuildingId;

    @Column(name = "to_building_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID toBuildingId;

    @Column(name = "walking_time_minutes", nullable = false)
    private int walkingTimeMinutes;

    @Convert(converter = JsonStringListConverter.class)
    @Column(name = "passing_venue_ids", columnDefinition = "JSON")
    private List<String> passingVenueIds;

    @Column(columnDefinition = "TEXT")
    private String description;
}
