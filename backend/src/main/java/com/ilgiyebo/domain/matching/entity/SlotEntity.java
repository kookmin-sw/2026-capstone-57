package com.ilgiyebo.domain;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.config.JsonMapConverter;
import com.ilgiyebo.config.JsonStringListConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "SLOT")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class SlotEntity extends BaseSchema {

    @Column(name = "user_id", columnDefinition = "BINARY(16)", nullable = false)
    private UUID userId;

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "JSON")
    private List<String> hobbies;

    @Convert(converter = JsonStringListConverter.class)
    @Column(columnDefinition = "JSON")
    private List<String> interests;

    @Convert(converter = JsonMapConverter.class)
    @Column(name = "ideal_type", columnDefinition = "JSON")
    private Map<String, Object> idealType;

    @Column(name = "current_match_id", columnDefinition = "BINARY(16)")
    private UUID currentMatchId;

    @Builder.Default
    @Column(name = "is_quick_match", nullable = false)
    private boolean isQuickMatch = false;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SlotStatus status = SlotStatus.EMPTY;
}
