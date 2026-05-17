package com.ilgiyebo.domain.matching.service;

import com.ilgiyebo.domain.campus.entity.CampusBuildingEntity;
import com.ilgiyebo.domain.campus.repository.CampusPathRepository;
import com.ilgiyebo.domain.user.entity.ScheduleEntity;
import com.ilgiyebo.domain.matching.entity.SlotEntity;
import com.ilgiyebo.domain.matching.entity.SlotPriority;
import com.ilgiyebo.domain.matching.entity.SlotStatus;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.matching.exception.MatchingException;
import com.ilgiyebo.domain.matching.dto.BatchMatchingResultDto;
import com.ilgiyebo.domain.matching.dto.MatchedUserDto;
import com.ilgiyebo.domain.matching.dto.OverlapLocationDto;
import com.ilgiyebo.domain.matching.dto.RouteOverlapDto;
import com.ilgiyebo.domain.matching.dto.SlotResponseDto;
import com.ilgiyebo.domain.safety.repository.BlockRepository;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import com.ilgiyebo.domain.user.repository.ScheduleRepository;
import com.ilgiyebo.domain.matching.repository.SlotRepository;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingServiceImpl implements MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingServiceImpl.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final SlotRepository slotRepository;
    private final UserRepository userRepository;
    private final ScheduleRepository scheduleRepository;
    private final BlockRepository blockRepository;
    private final CampusPathRepository campusPathRepository;
    private final MatchRepository matchRepository;
    private final MatchingTransactionHelper matchingTransactionHelper;

    @Override
    @Transactional(readOnly = true)
    public List<SlotResponseDto> getSlots(UUID userId) {
        List<SlotEntity> slots = slotRepository.findByUserId(userId);

        return slots.stream()
                .map(slot -> {
                    if (slot.getCurrentMatch() == null) {
                        return SlotResponseDto.from(slot);
                    }
                    MatchedUserDto matchedUser = resolveMatchedUser(slot.getCurrentMatch().getId(), userId);
                    return SlotResponseDto.from(slot, matchedUser);
                })
                .toList();
    }

    @Override
    @Transactional
    public SlotResponseDto unlockSlot(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw MatchingException.USER_NOT_FOUND.toException();
        }

        // TODO: 슬롯 해금 시 경험치 조건 필요
        
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(MatchingException.USER_NOT_FOUND::toException);

        SlotEntity slot = SlotEntity.builder()
                .user(user)
                .priority(SlotPriority.HOBBY)
                .status(SlotStatus.EMPTY)
                .build();
        slot = slotRepository.save(slot);

        log.info("새 슬롯 해금: userId={}, slotId={}", userId, slot.getId());
        return SlotResponseDto.from(slot);
    }

    @Override
    @Transactional
    public SlotResponseDto updateSlotPriority(UUID userId, UUID slotId, SlotPriority priority) {
        SlotEntity slot = findSlotOrThrow(slotId);
        verifyOwnership(slot, userId);

        slot.setPriority(priority);
        slot = slotRepository.save(slot);

        return SlotResponseDto.from(slot);
    }

    @Override
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
            if (daySchedulesB == null) {
                continue;
            }
            for (ScheduleEntity sa : byDayA.get(day)) {
                for (ScheduleEntity sb : daySchedulesB) {
                    findOverlap(sa, sb, day).ifPresent(overlaps::add);
                }
            }
        }

        return new RouteOverlapDto(!overlaps.isEmpty(), overlaps);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(UUID userA, UUID userB) {
        return blockRepository.existsByUserIdAndBlockedUserId(userA, userB)
                || blockRepository.existsByUserIdAndBlockedUserId(userB, userA);
    }

    @Override
    public BatchMatchingResultDto executeBatchMatching() {
        LocalDate today = LocalDate.now();
        LocalDate cycleStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate cycleEnd = cycleStart.plusDays(4); // 금요일

        log.info("배치 매칭 시작: cycleStart={}, cycleEnd={}", cycleStart, cycleEnd);

        // 1. 빈 슬롯 조회 (읽기 전용 트랜잭션)
        List<SlotEntity> emptySlots = matchingTransactionHelper.findEmptySlots();
        if (emptySlots.isEmpty()) {
            log.info("빈 슬롯이 없어 배치 매칭을 종료합니다.");
            return new BatchMatchingResultDto(0, 0, 0, 0, List.of());
        }

        log.info("빈 슬롯 수: {}", emptySlots.size());

        // 2. 사용자별 빈 슬롯 ID 그룹핑 (엔티티 대신 ID로 관리)
        Map<UUID, List<UUID>> slotIdsByUser = emptySlots.stream()
                .collect(Collectors.groupingBy(
                        slot -> slot.getUser().getId(),
                        Collectors.mapping(SlotEntity::getId, Collectors.toList())
                ));

        // 3. 매칭 대상 사용자 목록 (시간표가 있는 사용자만)
        Set<UUID> candidateUsers = matchingTransactionHelper.filterCandidateUsers(slotIdsByUser.keySet());

        if (candidateUsers.size() < 2) {
            log.info("매칭 가능한 사용자가 2명 미만입니다. candidateUsers={}", candidateUsers.size());
            return new BatchMatchingResultDto(emptySlots.size(), 0, 0, 0, List.of());
        }

        // 4. 매칭 실행 (개별 매칭마다 별도 트랜잭션)
        Set<UUID> matchedSlots = new HashSet<>();
        int matchesCreated = 0;
        List<String> failedSlots = new ArrayList<>();

        List<UUID> userList = new ArrayList<>(candidateUsers);

        for (int i = 0; i < userList.size(); i++) {
            UUID userA = userList.get(i);
            List<UUID> userASlotIds = slotIdsByUser.get(userA);
            if (userASlotIds == null) continue;

            for (UUID slotAId : userASlotIds) {
                if (matchedSlots.contains(slotAId)) continue;

                boolean matched = false;
                for (int j = i + 1; j < userList.size(); j++) {
                    UUID userB = userList.get(j);
                    List<UUID> userBSlotIds = slotIdsByUser.get(userB);
                    if (userBSlotIds == null) continue;

                    // 차단 관계 확인
                    if (matchingTransactionHelper.isBlocked(userA, userB)) continue;

                    for (UUID slotBId : userBSlotIds) {
                        if (matchedSlots.contains(slotBId)) continue;

                        // 동선 겹침 계산
                        RouteOverlapDto overlap = matchingTransactionHelper.calculateRouteOverlap(userA, userB);
                        if (!overlap.hasOverlap()) continue;

                        // 개별 매칭을 독립 트랜잭션으로 실행
                        try {
                            UUID matchId = matchingTransactionHelper.createSingleMatch(
                                    userA, userB, slotAId, slotBId, cycleStart, cycleEnd);

                            // DB 커밋 완료 후 SQS 발행 (실패해도 매칭은 유지)
                            matchingTransactionHelper.publishPostMatchEvents(matchId, userA, userB);

                            matchedSlots.add(slotAId);
                            matchedSlots.add(slotBId);
                            matchesCreated++;
                            matched = true;

                            log.info("매칭 성사: userA={}, userB={}, slotA={}, slotB={}, matchId={}",
                                    userA, userB, slotAId, slotBId, matchId);
                        } catch (Exception e) {
                            log.error("개별 매칭 생성 실패: userA={}, userB={}, slotA={}, slotB={}",
                                    userA, userB, slotAId, slotBId, e);
                            failedSlots.add(slotAId.toString());
                        }
                        break;
                    }
                    if (matched) break;
                }

                if (!matched && !failedSlots.contains(slotAId.toString())) {
                    failedSlots.add(slotAId.toString());
                }
            }
        }

        log.info("배치 매칭 완료: totalProcessed={}, matchesCreated={}", emptySlots.size(), matchesCreated);
        return new BatchMatchingResultDto(emptySlots.size(), matchesCreated, 0, matchesCreated, failedSlots);
    }

    /**
     * 매칭 ID와 현재 사용자 ID로부터 상대방의 요약 정보를 조회한다.
     */
    private MatchedUserDto resolveMatchedUser(UUID matchId, UUID currentUserId) {
        return matchRepository.findById(matchId)
                .map(match -> {
                    UserEntity partner = match.getUserA().getId().equals(currentUserId)
                            ? match.getUserB()
                            : match.getUserA();
                    return new MatchedUserDto(partner.getId(), partner.getNickname());
                })
                .orElse(null);
    }

    /**
     * 두 스케줄 항목의 시간 겹침과 건물 일치/인접 여부를 확인한다.
     * 같은 건물이거나 CampusPath로 연결된 인접 건물이면 겹침으로 판정한다.
     * campusBuilding이 파싱되지 않은 스케줄은 비교 대상에서 제외한다.
     */
    private Optional<OverlapLocationDto> findOverlap(ScheduleEntity sa, ScheduleEntity sb, DayOfWeek day) {
        LocalTime overlapStart = sa.getStartedAt().isAfter(sb.getStartedAt()) ? sa.getStartedAt() : sb.getStartedAt();
        LocalTime overlapEnd = sa.getEndedAt().isBefore(sb.getEndedAt()) ? sa.getEndedAt() : sb.getEndedAt();

        if (!overlapStart.isBefore(overlapEnd)) {
            return Optional.empty();
        }

        CampusBuildingEntity buildingA = sa.getCampusBuilding();
        CampusBuildingEntity buildingB = sb.getCampusBuilding();

        // 건물 정보가 파싱되지 않은 경우 비교 불가
        if (buildingA == null || buildingB == null) {
            return Optional.empty();
        }

        String timeRange = day.name() + " " + overlapStart.format(TIME_FMT) + "~" + overlapEnd.format(TIME_FMT);

        if (buildingA.getId().equals(buildingB.getId())) {
            // 같은 건물
            return Optional.of(new OverlapLocationDto(buildingA.getName(), buildingB.getName(), timeRange));
        }

        if (areAdjacentBuildings(buildingA.getId(), buildingB.getId())) {
            // 인접 건물 (campus_path로 연결)
            return Optional.of(new OverlapLocationDto(buildingA.getName(), buildingB.getName(), timeRange));
        }

        return Optional.empty();
    }

    /**
     * 두 건물이 CampusPath로 연결된 인접 건물인지 확인한다.
     */
    private boolean areAdjacentBuildings(UUID buildingIdA, UUID buildingIdB) {
        return !campusPathRepository.findByFromBuildingIdAndToBuildingId(buildingIdA, buildingIdB).isEmpty()
                || !campusPathRepository.findByFromBuildingIdAndToBuildingId(buildingIdB, buildingIdA).isEmpty();
    }

    private SlotEntity findSlotOrThrow(UUID slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(MatchingException.SLOT_NOT_FOUND::toException);
    }

    private void verifyOwnership(SlotEntity slot, UUID userId) {
        if (!slot.getUser().getId().equals(userId)) {
            throw MatchingException.SLOT_NOT_OWNED.toException();
        }
    }
}
