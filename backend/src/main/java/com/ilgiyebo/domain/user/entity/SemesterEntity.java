package com.ilgiyebo.domain.user.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

@Entity
@Table(name = "semester", uniqueConstraints = {
        @UniqueConstraint(name = "uk_semester_year_term", columnNames = {"year", "term"})
})
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class SemesterEntity extends BaseSchema {

    @Column(name = "year", nullable = false)
    private int year;

    @Enumerated(EnumType.STRING)
    @Column(name = "term", nullable = false, length = 10)
    private SemesterTerm term;

    @Column(name = "started_at", nullable = false)
    private LocalDate startedAt;

    @Column(name = "ended_at", nullable = false)
    private LocalDate endedAt;

    /**
     * 주어진 날짜가 이 학기 기간에 포함되는지 확인한다.
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(startedAt) && !date.isAfter(endedAt);
    }
}
