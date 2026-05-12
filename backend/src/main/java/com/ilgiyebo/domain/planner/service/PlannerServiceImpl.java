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
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @Transactional
    public List<PlanEntryResponse> getPlanEntries(UUID userId, LocalDate date) {
        List<PlanEntryEntity> entries = planEntryRepository.findByUserIdAndDateOrderByStartTimeAsc(userId, date);

        // Lazy 생성: 미래 날짜인데 SCHEDULE_AUTO가 없으면 해당 날짜만 생성
        if (!date.isBefore(LocalDate.now()) && entries.stream().noneMatch(e -> e.getSource() == PlanSource.SCHEDULE_AUTO)) {
            List<PlanEntryEntity> generated = lazyGenerateForDate(userId, date);
            if (!generated.isEmpty()) {
                entries = planEntryRepository.findByUserIdAndDateOrderByStartTimeAsc(userId, date);
            }
        }

        return entries.stream()
                .map(PlanEntryResponse::from)
                .toList();
    }

    /**
     * 특정 날짜에 대해 SCHEDULE_AUTO PLAN_ENTRY를 lazy 생성한다.
     * 주간 배치가 아직 실행되지 않은 미래 날짜를 조회할 때 fallback으로 사용한다.
     */
    private List<PlanEntryEntity> lazyGenerateForDate(UUID userId, LocalDate date) {
        LocalDate today = LocalDate.now();
        SemesterEntity semester = semesterRepository.findCurrentByDate(today).orElse(null);
        if (semester == null || date.isAfter(semester.getEndedAt())) {
            return List.of();
        }

        List<ScheduleEntity> schedules = scheduleRepository.findAllByUserIdAndSemesterId(userId, semester.getId());
        if (schedules.isEmpty()) {
            return List.of();
        }

        UserEntity user = findUserOrThrow(userId);
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        // 해당 날짜의 기존 MANUAL/SCHEDULE_OVERRIDE 조회 (충돌 검사용)
        List<PlanEntryEntity> existing = planEntryRepository.findByUserIdAndDateBetweenAndSourceIn(
                userId, date, date, List.of(PlanSource.MANUAL, PlanSource.SCHEDULE_OVERRIDE));

        List<PlanEntryEntity> generated = new ArrayList<>();
        for (ScheduleEntity schedule : schedules) {
            if (schedule.getDayOfWeek() == dayOfWeek) {
                if (!hasTimeConflict(existing, schedule.getStartedAt(), schedule.getEndedAt())) {
                    generated.add(PlanEntryEntity.builder()
                            .user(user)
                            .date(date)
                            .startTime(schedule.getStartedAt())
                            .endTime(schedule.getEndedAt())
                            .location(schedule.getPlace())
                            .name(schedule.getName())
                            .type(PlanItemType.CLASS)
                            .source(PlanSource.SCHEDULE_AUTO)
                            .sourceSchedule(schedule)
                            .build());
                }
            }
        }

        if (!generated.isEmpty()) {
            planEntryRepository.saveAll(generated);
            log.debug("Lazy PLAN_ENTRY 생성: userId={}, date={}, count={}", userId, date, generated.size());
        }
        return generated;
    }

    /**
     * 현재 주(이번 주) 기준으로만 SCHEDULE_AUTO PLAN_ENTRY를 생성하는 내부 메서드.
     * 시간표 등록/재등록 시 호출된다.
     * 나머지 주차는 주간 배치 스케줄러 또는 조회 시 lazy 생성으로 처리한다.
     */
    @Transactional
    private ScheduleAutoGenerateResult generatePlanEntriesFromSchedule(UUID userId, UUID semesterId, LocalDate semesterStart, LocalDate semesterEnd) {
        UserEntity user = findUserOrThrow(userId);

        // 1. 기존 SCHEDULE_AUTO 미래 일정 삭제
        LocalDate today = LocalDate.now();
        LocalDate deleteFrom = today.isAfter(semesterStart) ? today : semesterStart;
        int deleted = planEntryRepository.deleteByUserIdAndSourceAndDateAfter(
                userId, PlanSource.SCHEDULE_AUTO, deleteFrom);
        log.info("기존 SCHEDULE_AUTO 일정 삭제: userId={}, deleted={}", userId, deleted);

        // 2. 현재 학기의 시간표만 조회
        List<ScheduleEntity> schedules = scheduleRepository.findAllByUserIdAndSemesterId(userId, semesterId);
        if (schedules.isEmpty()) {
            log.info("시간표가 없어 PLAN_ENTRY 자동 생성을 건너뜁니다: userId={}", userId);
            return ScheduleAutoGenerateResult.success(0);
        }

        // 3. 현재 주(이번 주 월~일)만 즉시 생성
        LocalDate weekStart = deleteFrom;
        LocalDate weekEnd = today.with(DayOfWeek.SUNDAY);
        if (weekEnd.isAfter(semesterEnd)) {
            weekEnd = semesterEnd;
        }

        ScheduleAutoGenerateResult result = generateWeekEntries(user, userId, schedules, weekStart, weekEnd);
        log.info("시간표 등록 후 현재 주 PLAN_ENTRY 생성: userId={}, created={}, skipped={}",
                userId, result.createdCount(), result.skippedCount());

        return result;
    }

    /**
     * 특정 주 범위의 SCHEDULE_AUTO PLAN_ENTRY를 생성한다.
     * 주간 배치 스케줄러와 시간표 등록 시 공통으로 사용한다.
     * 기존 MANUAL/SCHEDULE_OVERRIDE와 충돌하는 일정은 skip한다.
     */
    @Transactional
    public ScheduleAutoGenerateResult generateWeekEntries(UserEntity user, UUID userId,
                                                          List<ScheduleEntity> schedules,
                                                          LocalDate weekStart, LocalDate weekEnd) {
        // 해당 주의 기존 MANUAL/SCHEDULE_OVERRIDE 일정을 한 번에 조회
        List<PlanEntryEntity> existingEntries = planEntryRepository.findByUserIdAndDateBetweenAndSourceIn(
                userId, weekStart, weekEnd,
                List.of(PlanSource.MANUAL, PlanSource.SCHEDULE_OVERRIDE));

        // 날짜별로 그룹핑하여 메모리에서 빠르게 충돌 검사
        Map<LocalDate, List<PlanEntryEntity>> existingByDate = existingEntries.stream()
                .collect(Collectors.groupingBy(PlanEntryEntity::getDate));

        List<PlanEntryEntity> entries = new ArrayList<>();
        List<ScheduleAutoGenerateResult.SkippedSchedule> skipped = new ArrayList<>();

        LocalDate current = weekStart;
        while (!current.isAfter(weekEnd)) {
            DayOfWeek dayOfWeek = current.getDayOfWeek();
            List<PlanEntryEntity> dayEntries = existingByDate.getOrDefault(current, List.of());

            for (ScheduleEntity schedule : schedules) {
                if (schedule.getDayOfWeek() == dayOfWeek) {
                    // 메모리 충돌 검사
                    if (hasTimeConflict(dayEntries, schedule.getStartedAt(), schedule.getEndedAt())) {
                        skipped.add(new ScheduleAutoGenerateResult.SkippedSchedule(
                                current,
                                schedule.getName(),
                                schedule.getStartedAt(),
                                schedule.getEndedAt(),
                                "CONFLICT_WITH_EXISTING_PLAN"
                        ));
                        continue;
                    }

                    entries.add(PlanEntryEntity.builder()
                            .user(user)
                            .date(current)
                            .startTime(schedule.getStartedAt())
                            .endTime(schedule.getEndedAt())
                            .location(schedule.getPlace())
                            .name(schedule.getName())
                            .type(PlanItemType.CLASS)
                            .source(PlanSource.SCHEDULE_AUTO)
                            .sourceSchedule(schedule)
                            .build());
                }
            }
            current = current.plusDays(1);
        }

        planEntryRepository.saveAll(entries);
        return new ScheduleAutoGenerateResult(entries.size(), skipped.size(), skipped);
    }

    /**
     * 다음 주 PLAN_ENTRY를 배치 생성한다.
     * 주간 스케줄러에서 호출한다.
     */
    @Override
    @Transactional
    public ScheduleAutoGenerateResult generateNextWeekEntries(UUID userId) {
        LocalDate today = LocalDate.now();
        SemesterEntity semester = semesterRepository.findCurrentByDate(today)
                .orElseThrow(() -> PlannerException.SEMESTER_NOT_FOUND.toException());

        List<ScheduleEntity> schedules = scheduleRepository.findAllByUserIdAndSemesterId(userId, semester.getId());
        if (schedules.isEmpty()) {
            return ScheduleAutoGenerateResult.success(0);
        }

        UserEntity user = findUserOrThrow(userId);
        LocalDate nextMonday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LocalDate nextSunday = nextMonday.plusDays(6);
        if (nextSunday.isAfter(semester.getEndedAt())) {
            nextSunday = semester.getEndedAt();
        }
        if (nextMonday.isAfter(semester.getEndedAt())) {
            return ScheduleAutoGenerateResult.success(0);
        }

        return generateWeekEntries(user, userId, schedules, nextMonday, nextSunday);
    }

    /**
     * 메모리에서 시간 충돌을 검사한다.
     * 기존 일정 중 하나라도 새 시간 범위와 겹치면 true를 반환한다.
     * 충돌 조건: existing.startTime < newEndTime AND existing.endTime > newStartTime
     */
    private boolean hasTimeConflict(List<PlanEntryEntity> existingEntries, LocalTime newStart, LocalTime newEnd) {
        for (PlanEntryEntity existing : existingEntries) {
            if (existing.getStartTime().isBefore(newEnd) && existing.getEndTime().isAfter(newStart)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional
    public ScheduleAutoGenerateResult regenerateScheduleAutoEntries(UUID userId) {
        // 현재 활성 학기를 DB에서 조회
        LocalDate today = LocalDate.now();
        SemesterEntity semester = semesterRepository.findCurrentByDate(today)
                .orElseThrow(() -> PlannerException.SEMESTER_NOT_FOUND.toException());

        return generatePlanEntriesFromSchedule(userId, semester.getId(), semester.getStartedAt(), semester.getEndedAt());
    }

    @Override
    @Transactional
    public void deleteScheduleLinkedEntries(UUID userId, UUID semesterId) {
        // SCHEDULE_OVERRIDE의 FK를 null로 설정 (일정 자체는 보존)
        int detached = planEntryRepository.detachSourceScheduleForOverrides(userId, PlanSource.SCHEDULE_OVERRIDE, semesterId);
        // SCHEDULE_AUTO만 삭제
        int deleted = planEntryRepository.deleteByUserIdAndSourceScheduleNotNull(userId, PlanSource.SCHEDULE_AUTO, semesterId);
        log.info("시간표 연결 해제: userId={}, semesterId={}, deleted={}, detached={}", userId, semesterId, deleted, detached);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean shouldSendInactivityReminder(UUID userId) {
        LocalDate today = LocalDate.now();
        LocalDate threeDaysAgo = today.minusDays(3);
        return !planEntryRepository.existsEntryBySourceBetween(userId, PlanSource.MANUAL, threeDaysAgo, today);
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
