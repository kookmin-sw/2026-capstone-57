package com.ilgiyebo.domain.matching.service;

import com.ilgiyebo.domain.campus.entity.CampusBuildingEntity;
import com.ilgiyebo.domain.campus.repository.CampusPathRepository;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.dto.OverlapLocationDto;
import com.ilgiyebo.domain.matching.dto.RouteOverlapDto;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.entity.MatchStatus;
import com.ilgiyebo.domain.matching.entity.SlotEntity;
import com.ilgiyebo.domain.matching.entity.SlotStatus;
import com.ilgiyebo.domain.matching.exception.MatchingException;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import com.ilgiyebo.domain.matching.repository.SlotRepository;
import com.ilgiyebo.domain.mission.service.MissionService;
import com.ilgiyebo.domain.safety.repository.BlockRepository;
import com.ilgiyebo.domain.user.entity.ScheduleEntity;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.user.repository.ScheduleRepository;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 배치 매칭에서 개별 매칭 생성을 독립 트랜잭션으로 분리하기 위한 헬퍼.
 * Spring AOP 프록시 특성상 같은 클래스 내부 호출은 트랜잭션이 적용되지 않으므로
 * 별도 빈으로 분리한다.
 */
@Component
@RequiredArgsConstructor
public class MatchingTransactionHelper {

    private static final Logger log = LoggerFactory.getLogger(MatchingTransactionHelper.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final BlockRepository blockRepository;
    private final CampusPathRepository campusPathRepository;
    private final MatchRepository matchRepository;
    private final InteractionRepository interactionRepository;
    private final MissionService missionService;

    /**
     * 빈 슬롯을 조회한다 (읽기 전용 트랜잭션).
     */
    @Transactional(readOnly = true)
    public List<SlotEntity> findEmptySlots() {
        return slotRepository.findByStatus(SlotStatus.EMPTY);
    }

    /**
     * 시간표가 있는 사용자만 필터링한다 (읽기 전용 트랜잭션).
     */
    @Transactional(readOnly = true)
    public Set<UUID> filterCandidateUsers(Set<UUID> userIds) {
        return userIds.stream()
                .filter(userId -> !scheduleRepository.findAllByUserId(userId).isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 두 사용자 간 차단 관계를 확인한다 (읽기 전용 트랜잭션).
     */
    @Transactional(readOnly = true)
    public boolean isBlocked(UUID userA, UUID userB) {
        return blockRepository.existsByUserIdAndBlockedUserId(userA, userB)
                || blockRepository.existsByUserIdAndBlockedUserId(userB, userA);
    }

    /**
     * 두 사용자의 동선 겹침을 계산한다 (읽기 전용 트랜잭션).
     * 트랜잭션 내에서 실행되므로 Lazy 프록시(campusBuilding)를 안전하게 초기화할 수 있다.
     */
    @Transactional(readOnly = true)
    public RouteOverlapDto calculateRouteOverlap(UUID userA, UUID userB) {
        List<ScheduleEntity> schedulesA = scheduleRepository.findAllByUserId(userA);
        List<ScheduleEntity> schedulesB = scheduleRepository.findAllByUserId(userB);

        if (schedulesA.isEmpty() || schedulesB.isEmpty()) {
            return new RouteOverlapDto(false, List.of());
        }

        Map<DayOfWeek, List<ScheduleEntity>> byDayA = schedulesA.stream()
                .collect(Collectors.groupingBy(ScheduleEntity::getDayOfWeek));
        Map<DayOfWeek, List<ScheduleEntity>> byDayB = schedulesB.stream()
                .collect(Collectors.groupingBy(ScheduleEntity::getDayOfWeek));

        List<OverlapLocationDto> overlaps = new ArrayList<>();

        for (DayOfWeek day : byDayA.keySet()) {
            List<ScheduleEntity> daySchedulesB = byDayB.get(day);
            if (daySchedulesB == null) continue;
            for (ScheduleEntity sa : byDayA.get(day)) {
                for (ScheduleEntity sb : daySchedulesB) {
                    findOverlap(sa, sb, day).ifPresent(overlaps::add);
                }
            }
        }

        return new RouteOverlapDto(!overlaps.isEmpty(), overlaps);
    }

    private Optional<OverlapLocationDto> findOverlap(ScheduleEntity sa, ScheduleEntity sb, DayOfWeek day) {
        LocalTime overlapStart = sa.getStartedAt().isAfter(sb.getStartedAt()) ? sa.getStartedAt() : sb.getStartedAt();
        LocalTime overlapEnd = sa.getEndedAt().isBefore(sb.getEndedAt()) ? sa.getEndedAt() : sb.getEndedAt();

        if (!overlapStart.isBefore(overlapEnd)) {
            return Optional.empty();
        }

        CampusBuildingEntity buildingA = sa.getCampusBuilding();
        CampusBuildingEntity buildingB = sb.getCampusBuilding();

        if (buildingA == null || buildingB == null) {
            return Optional.empty();
        }

        String timeRange = day.name() + " " + overlapStart.format(TIME_FMT) + "~" + overlapEnd.format(TIME_FMT);

        if (buildingA.getId().equals(buildingB.getId())) {
            return Optional.of(new OverlapLocationDto(buildingA.getName(), buildingB.getName(), timeRange));
        }

        if (areAdjacentBuildings(buildingA.getId(), buildingB.getId())) {
            return Optional.of(new OverlapLocationDto(buildingA.getName(), buildingB.getName(), timeRange));
        }

        return Optional.empty();
    }

    private boolean areAdjacentBuildings(UUID buildingIdA, UUID buildingIdB) {
        return !campusPathRepository.findByFromBuildingIdAndToBuildingId(buildingIdA, buildingIdB).isEmpty()
                || !campusPathRepository.findByFromBuildingIdAndToBuildingId(buildingIdB, buildingIdA).isEmpty();
    }

    /**
     * 단일 매칭을 독립 트랜잭션(REQUIRES_NEW)으로 생성한다.
     * 이 메서드가 실패해도 다른 매칭에는 영향을 주지 않는다.
     * SQS 발행은 트랜잭션 밖에서 별도로 수행해야 한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID createSingleMatch(UUID userAId, UUID userBId, UUID slotAId, UUID slotBId,
                                  LocalDate cycleStart, LocalDate cycleEnd) {
        SlotEntity slotA = slotRepository.findById(slotAId)
                .orElseThrow(MatchingException.SLOT_NOT_FOUND::toException);
        SlotEntity slotB = slotRepository.findById(slotBId)
                .orElseThrow(MatchingException.SLOT_NOT_FOUND::toException);

        UserEntity userA = userRepository.findById(userAId)
                .orElseThrow(MatchingException.USER_NOT_FOUND::toException);
        UserEntity userB = userRepository.findById(userBId)
                .orElseThrow(MatchingException.USER_NOT_FOUND::toException);

        // 매칭 엔티티 생성
        MatchEntity match = MatchEntity.builder()
                .userA(userA)
                .userB(userB)
                .slotA(slotA)
                .slotB(slotB)
                .cycleStartDate(cycleStart)
                .cycleEndDate(cycleEnd)
                .status(MatchStatus.ACTIVE)
                .isQuickMatch(false)
                .build();
        match = matchRepository.save(match);

        // 상호작용 생성
        InteractionEntity interaction = InteractionEntity.builder()
                .match(match)
                .currentStage(1)
                .stageStatus(StageStatus.IN_PROGRESS)
                .quizCompletedBy(List.of())
                .missionConfirmedBy(List.of())
                .reviewCompletedBy(List.of())
                .build();
        interactionRepository.save(interaction);

        // 슬롯 상태 업데이트
        slotA.setStatus(SlotStatus.ACTIVE);
        slotA.setCurrentMatch(match);
        slotRepository.save(slotA);

        slotB.setStatus(SlotStatus.ACTIVE);
        slotB.setCurrentMatch(match);
        slotRepository.save(slotB);

        return match.getId();
    }

    /**
     * 매칭 성사 후 외부 서비스(SQS)에 미션 생성을 요청한다.
     * 트랜잭션 밖에서 호출되므로 실패해도 DB에 영향 없음.
     */
    public void publishPostMatchEvents(UUID matchId, UUID userAId, UUID userBId) {
        requestMissionGenerationSafe(matchId);
    }

    private void requestMissionGenerationSafe(UUID matchId) {
        try {
            missionService.requestMissionGeneration(matchId);
        } catch (Exception e) {
            log.warn("미션 생성 요청 실패 (매칭은 유지): matchId={}", matchId, e);
        }
    }
}
