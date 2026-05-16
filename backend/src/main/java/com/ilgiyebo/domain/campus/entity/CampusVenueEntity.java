package com.ilgiyebo.domain.campus.entity;

import com.ilgiyebo.common.config.TypeActivityListConverter;
import com.ilgiyebo.common.entity.BaseSchema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Entity
@Table(name = "campus_venue")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class CampusVenueEntity extends BaseSchema {

    @Column(nullable = false, unique = true)
    private String name;

    @Convert(converter = TypeActivityListConverter.class)
    @Column(name = "type_activity", columnDefinition = "TEXT")
    private List<TypeActivity> typeActivity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "operating_hours")
    private String operatingHours;
}
