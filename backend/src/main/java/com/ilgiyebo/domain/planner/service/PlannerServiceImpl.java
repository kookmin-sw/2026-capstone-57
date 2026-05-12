package com.ilgiyebo.domain.planner.service;

import com.ilgiyebo.domain.planner.dto.PlanEntryRequest;
import com.ilgiyebo.domain.planner.dto.PlanEntryResponse;
import com.ilgiyebo.domain.planner.dto.ScheduleAutoGenerateResult;
import com.ilgiyebo.domain.planner.entity.PlanEntryEntity;
import com.ilgiyebo.domain.planner.entity.PlanItemType;
import com.ilgiyebo.domain.planner.entity.PlanSource;
import com.ilgiyebo.domain.planner.exception.PlannerException;
import com.ilgiyebo.domain.planner.repository.PlanEntryRepository;
import com.ilgiyebo.domain.user.entity.ScheduleEntity;
import com.ilgiyebo.domain.user.entity.SemesterEntity;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.user.repository.ScheduleRepository;
import com.ilgiyebo.domain.user.repository.SemesterRepository;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlannerServiceImpl implements PlannerService {

    private final PlanEntryRepository planEntryRepository;
    private final ScheduleRepository scheduleRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public PlanEntryResponse createPlanEntry(UUID userId, PlanEntryRequest request) {
        UserEntity user = findUserOrThrow(userId);

        validateTimeUnit(request.startTime());
        validateTimeUnit(request.endTime());
        validateTimeRange(request.startTime(), request.endTime());
        validateNoConflict(userId, request.date(), request.startTime(), request.endTime(), null);

        PlanEntryEntity entity = PlanEntryEntity.builder()
                .user(user)
                .date(request.date())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .location(request.location())
                .name(request.name())
                .type(request.type())
                .source(PlanSource.MANUAL)
                .build();

        entity = planEntryRepository.save(entity);
        log.debug("일정 생성: userId={}, entryId={}, date={}", userId, entity.getId(), request.date());

        // TODO: 경험치 하루 1회 지급 (ExperienceService 구현 후 연동)

        return PlanEntryResponse.from(entity);
    }

    @Override
    @Transactional
    public PlanEntryResponse updatePlanEntry(UUID userId, UUID entryId, PlanEntryRequest request) {
        PlanEntryEntity entity = findEntryOrThrow(entryId);
        verifyOwnership(entity, userId);

        validateTimeUnit(request.startTime());
        validateTimeUnit(request.endTime());
        validateTimeRange(request.startTime(), request.endTime());
        validateNoConflict(userId, request.date(), request.startTime(), request.endTime(), entryId);

        entity.setDate(request.date());
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        entity.setLocation(request.location());
        entity.setName(request.name());
        entity.setType(request.type());

        // SCHEDULE_AUTO 일정을 수정하면 SCHEDULE_OVERRIDE로 전환
        if (entity.getSource() == PlanSource.SCHEDULE_AUTO) {
            entity.setSource(PlanSource.SCHEDULE_OVERRIDE);
        }

        entity = planEntryRepository.save(entity);
        log.debug("일정 수정: userId={}, entryId={}", userId, entryId);

        return PlanEntryResponse.from(entity);
    }

    @Override
    @Transactional
    public void deletePlanEntry(UUID userId, UUID entryId) {
        PlanEntryEntity entity = findEntryOrThrow(entryId);
        verifyOwnership(entity, userId);

        planEntryRepository.delete(entity);
        log.debug("일정 삭제: userId={}, entryId={}", userId, entryId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanEntryResponse> getPlanEntries(UUID userId, LocalDate date) {
        return planEntryRepository.findByUserIdAndDateOrderByStartTimeAsc(userId, date)
                .stream()
                .map(PlanEntryResponse::from)
                .toList();
    }

    /**
     * 학기 범위 내 SCHEDULE_AUTO PLAN_ENTRY를 생성하는 내부 메서드.
     * MANUAL/SCHEDULE_OVERRIDE와 충돌하는 일정은 생성하지 않고 skip한다.
     */
    @Transactional
    private ScheduleAutoGenerateResult generatePlanEntriesFromSchedule(UUID userId, LocalDate semesterStart, LocalDate semesterEnd) {
        UserEntity user = findUserOrThrow(userId);

        // 1. 기존 SCHEDULE_AUTO 미래 일정 삭제
        LocalDate today = LocalDate.now();
        LocalDate deleteFrom = today.isAfter(semesterStart) ? today : semesterStart;
        int deleted = planEntryRepository.deleteByUserIdAndSourceAndDateAfter(
                userId, PlanSource.SCHEDULE_AUTO, deleteFrom);
        log.info("기존 SCHEDULE_AUTO 일정 삭제: userId={}, deleted={}", userId, deleted);

        // 2. 사용자의 시간표 조회
        List<ScheduleEntity> schedules = scheduleRepository.findAllByUserId(userId);
        if (schedules.isEmpty()) {
            log.info("시간표가 없어 PLAN_ENTRY 자동 생성을 건너뜁니다: userId={}", userId);
            return ScheduleAutoGenerateResult.success(0);
        }

        // 3. 학기 범위 내 날짜별 PLAN_ENTRY 생성 (충돌 검사 포함)
        List<PlanEntryEntity> entries = new ArrayList<>();
        List<ScheduleAutoGenerateResult.SkippedSchedule> skipped = new ArrayList<>();
        LocalDate current = deleteFrom;

        while (!current.isAfter(semesterEnd)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();

            for (ScheduleEntity schedule : schedules) {
                if (schedule.getDayOfWeek() == dayOfWeek) {
                    // 충돌 검사: MANUAL/SCHEDULE_OVERRIDE와 겹치는지 확인
                    boolean hasConflict = planEntryRepository.existsConflictingUserEntry(
                            userId, current, schedule.getStartedAt(), schedule.getEndedAt());

                    if (hasConflict) {
                        skipped.add(new ScheduleAutoGenerateResult.SkippedSchedule(
                                current,
                                schedule.getName(),
                                schedule.getStartedAt(),
                                schedule.getEndedAt(),
                                "CONFLICT_WITH_EXISTING_PLAN"
                        ));
                        continue;
                    }

                    PlanEntryEntity entry = PlanEntryEntity.builder()
                            .user(user)
                            .date(current)
                            .startTime(schedule.getStartedAt())
                            .endTime(schedule.getEndedAt())
                            .location(schedule.getPlace())
                            .name(schedule.getName())
                            .type(PlanItemType.CLASS)
                            .source(PlanSource.SCHEDULE_AUTO)
                            .sourceSchedule(schedule)
                            .build();
                    entries.add(entry);
                }
            }
            current = current.plusDays(1);
        }

        // Bulk save
        planEntryRepository.saveAll(entries);
        log.info("SCHEDULE_AUTO 일정 자동 생성 완료: userId={}, created={}, skipped={}",
                userId, entries.size(), skipped.size());

        return new ScheduleAutoGenerateResult(entries.size(), skipped.size(), skipped);
    }

    @Override
    @Transactional
    public ScheduleAutoGenerateResult regenerateScheduleAutoEntries(UUID userId) {
        // 현재 활성 학기를 DB에서 조회
        LocalDate today = LocalDate.now();
        SemesterEntity semester = semesterRepository.findCurrentByDate(today)
                .orElseThrow(() -> PlannerException.SEMESTER_NOT_FOUND.toException());

        return generatePlanEntriesFromSchedule(userId, semester.getStartedAt(), semester.getEndedAt());
    }

    @Override
    @Transactional
    public void deleteScheduleLinkedEntries(UUID userId) {
        // SCHEDULE_OVERRIDE의 FK를 null로 설정 (일정 자체는 보존)
        int detached = planEntryRepository.detachSourceScheduleForOverrides(userId);
        // SCHEDULE_AUTO만 삭제
        int deleted = planEntryRepository.deleteByUserIdAndSourceScheduleNotNull(userId);
        log.info("시간표 연결 해제: userId={}, deleted={}, detached={}", userId, deleted, detached);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendInactivityReminder(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysAgo = today.minusDays(3);
        return !planEntryRepository.existsManualEntryBetween(userId, threeDaysAgo, today);
    }

    // ===== Validation helpers =====

    /**
     * 30분 단위 검증: 분이 0 또는 30이어야 한다.
     */
    private void validateTimeUnit(LocalTime time) {
        if (time.getMinute() != 0 && time.getMinute() != 30) {
            throw PlannerException.INVALID_TIME_UNIT.toException();
        }
        if (time.getSecond() != 0 || time.getNano() != 0) {
            throw PlannerException.INVALID_TIME_UNIT.toException();
        }
    }

    /**
     * 종료 시간이 시작 시간보다 이후인지 검증.
     */
    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw PlannerException.INVALID_TIME_RANGE.toException();
        }
    }

    /**
     * 일정 시간 충돌 검증.
     * 겹치는 시간대 등록 불가. 단, 종료시간=시작시간 맞닿는 경우는 허용.
     */
    private void validateNoConflict(UUID userId, LocalDate date, LocalTime startTime, LocalTime endTime, UUID excludeId) {
        List<PlanEntryEntity> conflicts = planEntryRepository.findConflicting(
                userId, date, startTime, endTime, excludeId);
        if (!conflicts.isEmpty()) {
            throw PlannerException.TIME_CONFLICT.toException();
        }
    }

    // ===== Entity lookup helpers =====

    private UserEntity findUserOrThrow(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(PlannerException.USER_NOT_FOUND::toException);
    }

    private PlanEntryEntity findEntryOrThrow(UUID entryId) {
        return planEntryRepository.findById(entryId)
                .orElseThrow(PlannerException.ENTRY_NOT_FOUND::toException);
    }

    private void verifyOwnership(PlanEntryEntity entity, UUID userId) {
        if (!entity.getUser().getId().equals(userId)) {
            throw PlannerException.ENTRY_NOT_OWNED.toException();
        }
    }
}
