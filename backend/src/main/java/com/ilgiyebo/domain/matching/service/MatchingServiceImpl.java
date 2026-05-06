package com.ilgiyebo.service;

import com.ilgiyebo.domain.campus.entity.CampusBuildingEntity;
import com.ilgiyebo.domain.InteractionEntity;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.MatchStatus;
import com.ilgiyebo.domain.MissionEntity;
import com.ilgiyebo.domain.MissionStatus;
import com.ilgiyebo.domain.ScheduleEntity;
import com.ilgiyebo.domain.SlotEntity;
import com.ilgiyebo.domain.SlotPriority;
import com.ilgiyebo.domain.SlotStatus;
import com.ilgiyebo.domain.StageStatus;
import com.ilgiyebo.domain.campus.entity.CampusBuildingPlaceEntity;
import com.ilgiyebo.domain.matching.exception.MatchingException;
import com.ilgiyebo.dto.BatchMatchingResultDto;
import com.ilgiyebo.dto.MatchedUserDto;
import com.ilgiyebo.dto.OverlapLocationDto;
import com.ilgiyebo.dto.RouteOverlapDto;
import com.ilgiyebo.dto.SlotResponseDto;
import com.ilgiyebo.repository.BlockRepository;
import com.ilgiyebo.domain.campus.repository.CampusBuildingRepository;
import com.ilgiyebo.domain.campus.repository.CampusPathRepository;
import com.ilgiyebo.domain.campus.repository.PlaceRepository;
import com.ilgiyebo.repository.InteractionRepository;
import com.ilgiyebo.repository.MatchRepository;
import com.ilgiyebo.repository.MissionRepository;
import com.ilgiyebo.repository.ScheduleRepository;
import com.ilgiyebo.repository.SlotRepository;
import com.ilgiyebo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Instant;
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
    private final CampusBuildingRepository campusBuildingRepository;
    private final CampusPathRepository campusPathRepository;
    private final PlaceRepository placeRepository;
    private final MatchRepository matchRepository;
    private final MissionRepository missionRepository;
    private final InteractionRepository interactionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<SlotResponseDto> getSlots(UUID userId) {
        List<SlotEntity> slots = slotRepository.findByUserId(userId);

        return slots.stream()
                .map(slot -> {
                    if (slot.getCurrentMatchId() == null) {
                        return SlotResponseDto.from(slot);
                    }
                    MatchedUserDto matchedUser = resolveMatchedUser(slot.getCurrentMatchId(), userId);
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

        SlotEntity slot = SlotEntity.builder()
                .userId(userId)
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
    @Transactional
    public BatchMatchingResultDto executeBatchMatching() {
        LocalDate today = LocalDate.now();
        LocalDate cycleStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate cycleEnd = cycleStart.plusDays(4); // 금요일

        log.info("배치 매칭 시작: cycleStart={}, cycleEnd={}", cycleStart, cycleEnd);

        // 1. 빈 슬롯 조회
        List<SlotEntity> emptySlots = slotRepository.findByStatus(SlotStatus.EMPTY);
        if (emptySlots.isEmpty()) {
            log.info("빈 슬롯이 없어 배치 매칭을 종료합니다.");
            return new BatchMatchingResultDto(0, 0, 0, 0, List.of());
        }

        log.info("빈 슬롯 수: {}", emptySlots.size());

        // 2. 사용자별 빈 슬롯 그룹핑
        Map<UUID, List<SlotEntity>> slotsByUser = emptySlots.stream()
                .collect(Collectors.groupingBy(SlotEntity::getUserId));

        // 3. 매칭 대상 사용자 목록 (시간표가 있는 사용자만)
        Set<UUID> candidateUsers = new HashSet<>();
        for (UUID userId : slotsByUser.keySet()) {
            List<ScheduleEntity> schedules = scheduleRepository.findAllByUserId(userId);
            if (!schedules.isEmpty()) {
                candidateUsers.add(userId);
            }
        }

        if (candidateUsers.size() < 2) {
            log.info("매칭 가능한 사용자가 2명 미만입니다. candidateUsers={}", candidateUsers.size());
            return new BatchMatchingResultDto(emptySlots.size(), 0, 0, 0, List.of());
        }

        // 4. 매칭 실행
        Set<UUID> matchedSlots = new HashSet<>();
        int matchesCreated = 0;
        List<String> failedSlots = new ArrayList<>();

        List<UUID> userList = new ArrayList<>(candidateUsers);

        for (int i = 0; i < userList.size(); i++) {
            UUID userA = userList.get(i);
            List<SlotEntity> userASlots = slotsByUser.get(userA);
            if (userASlots == null) continue;

            for (SlotEntity slotA : userASlots) {
                if (matchedSlots.contains(slotA.getId())) continue;

                boolean matched = false;
                for (int j = i + 1; j < userList.size(); j++) {
                    UUID userB = userList.get(j);
                    List<SlotEntity> userBSlots = slotsByUser.get(userB);
                    if (userBSlots == null) continue;

                    // 차단 관계 확인
                    if (isBlocked(userA, userB)) continue;

                    for (SlotEntity slotB : userBSlots) {
                        if (matchedSlots.contains(slotB.getId())) continue;

                        // 동선 겹침 계산
                        RouteOverlapDto overlap = calculateRouteOverlap(userA, userB);
                        if (!overlap.hasOverlap()) continue;

                        // 겹치는 동선 중 하나를 선택
                        OverlapLocationDto selectedOverlap = overlap.overlappingLocations().get(0);

                        // 매칭 성사
                        MatchEntity match = createMatch(userA, userB, slotA, slotB, cycleStart, cycleEnd);

                        // 상호작용 생성
                        createInteraction(match.getId());

                        // 미션 사전 생성 (선택된 동선 기반)
                        createMissionFromOverlap(match.getId(), selectedOverlap, cycleEnd);

                        // 슬롯 상태 업데이트
                        slotA.setStatus(SlotStatus.ACTIVE);
                        slotA.setCurrentMatchId(match.getId());
                        slotRepository.save(slotA);

                        slotB.setStatus(SlotStatus.ACTIVE);
                        slotB.setCurrentMatchId(match.getId());
                        slotRepository.save(slotB);

                        matchedSlots.add(slotA.getId());
                        matchedSlots.add(slotB.getId());
                        matchesCreated++;
                        matched = true;

                        log.info("매칭 성사: userA={}, userB={}, matchId={}, overlap={}",
                                userA, userB, match.getId(), selectedOverlap);
                        break;
                    }
                    if (matched) break;
                }

                if (!matched) {
                    failedSlots.add(slotA.getId().toString());
                }
            }
        }

        log.info("배치 매칭 완료: totalProcessed={}, matchesCreated={}", emptySlots.size(), matchesCreated);
        return new BatchMatchingResultDto(emptySlots.size(), matchesCreated, 0, matchesCreated, failedSlots);
    }

    /**
     * 매칭 엔티티를 생성하고 저장한다.
     */
    private MatchEntity createMatch(UUID userA, UUID userB, SlotEntity slotA, SlotEntity slotB,
                                    LocalDate cycleStart, LocalDate cycleEnd) {
        MatchEntity match = MatchEntity.builder()
                .userAId(userA)
                .userBId(userB)
                .slotAId(slotA.getId())
                .slotBId(slotB.getId())
                .cycleStartDate(cycleStart)
                .cycleEndDate(cycleEnd)
                .status(MatchStatus.ACTIVE)
                .isQuickMatch(false)
                .build();
        return matchRepository.save(match);
    }

    /**
     * 매칭에 대한 상호작용 엔티티를 생성한다.
     */
    private void createInteraction(UUID matchId) {
        InteractionEntity interaction = InteractionEntity.builder()
                .matchId(matchId)
                .currentStage(1)
                .stageStatus(StageStatus.IN_PROGRESS)
                .quizCompletedBy(List.of())
                .missionConfirmedBy(List.of())
                .reviewCompletedBy(List.of())
                .build();
        interactionRepository.save(interaction);
    }

    /**
     * 선택된 동선 겹침 정보를 기반으로 4단계 미션 데이터를 사전 생성한다.
     * 겹침 장소 인근의 장소(카페, 매점 등)를 조회하여 미션 장소로 설정한다.
     */
    private void createMissionFromOverlap(UUID matchId, OverlapLocationDto overlap, LocalDate cycleEnd) {
        String location = overlap.fromBuilding();
        String activity = "만남";

        // 겹침 장소 인근 장소 조회 시도
        Optional<CampusBuildingEntity> building = campusBuildingRepository.findByName(overlap.fromBuilding());
        if (building.isPresent()) {
            List<CampusBuildingPlaceEntity> places = placeRepository.findByBuildingId(building.get().getId());
            if (!places.isEmpty()) {
                // 첫 번째 장소를 미션 장소로 선택
                CampusBuildingPlaceEntity selectedPlace = places.get(0);
                location = selectedPlace.getName();
                activity = selectedPlace.getType() + "에서 만남";
            }
        }

        // 미션 기한: 매칭 주기 종료일(금요일) 23:59:59
        Instant deadline = cycleEnd.plusDays(1).atStartOfDay()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .minusSeconds(1);

        MissionEntity mission = MissionEntity.builder()
                .matchId(matchId)
                .location(location)
                .activity(activity)
                .description(overlap.fromBuilding() + " 근처에서 " + overlap.timeRange() + " 시간대에 만남")
                .deadline(deadline)
                .confirmedBy(List.of())
                .extended(false)
                .status(MissionStatus.PENDING)
                .build();
        missionRepository.save(mission);

        log.info("미션 사전 생성: matchId={}, location={}, activity={}", matchId, location, activity);
    }

    /**
     * 매칭 ID와 현재 사용자 ID로부터 상대방의 요약 정보를 조회한다.
     */
    private MatchedUserDto resolveMatchedUser(UUID matchId, UUID currentUserId) {
        return matchRepository.findById(matchId)
                .map(match -> {
                    UUID partnerId = match.getUserAId().equals(currentUserId)
                            ? match.getUserBId()
                            : match.getUserAId();
                    return userRepository.findById(partnerId)
                            .map(user -> new MatchedUserDto(user.getId(), user.getNickname()))
                            .orElse(null);
                })
                .orElse(null);
    }

    /**
     * 두 스케줄 항목의 시간 겹침과 장소 일치/인접 여부를 확인한다.
     * 같은 건물이거나 CampusPath로 연결된 인접 건물이면 겹침으로 판정한다.
     */
    private Optional<OverlapLocationDto> findOverlap(ScheduleEntity sa, ScheduleEntity sb, DayOfWeek day) {
        LocalTime overlapStart = sa.getStartedAt().isAfter(sb.getStartedAt()) ? sa.getStartedAt() : sb.getStartedAt();
        LocalTime overlapEnd = sa.getEndedAt().isBefore(sb.getEndedAt()) ? sa.getEndedAt() : sb.getEndedAt();

        if (!overlapStart.isBefore(overlapEnd)) {
            return Optional.empty();
        }

        String placeA = sa.getPlace();
        String placeB = sb.getPlace();

        if (placeA.equals(placeB)) {
            String timeRange = day.name() + " " + overlapStart.format(TIME_FMT) + "~" + overlapEnd.format(TIME_FMT);
            return Optional.of(new OverlapLocationDto(placeA, placeB, timeRange));
        }

        if (areAdjacentBuildings(placeA, placeB)) {
            String timeRange = day.name() + " " + overlapStart.format(TIME_FMT) + "~" + overlapEnd.format(TIME_FMT);
            return Optional.of(new OverlapLocationDto(placeA, placeB, timeRange));
        }

        return Optional.empty();
    }

    /**
     * 두 건물이 CampusPath로 연결된 인접 건물인지 확인한다.
     * 건물 이름으로 CampusBuilding을 조회한 뒤, 양방향 경로 존재 여부를 확인한다.
     */
    private boolean areAdjacentBuildings(String placeA, String placeB) {
        Optional<CampusBuildingEntity> buildingA = campusBuildingRepository.findByName(placeA);
        Optional<CampusBuildingEntity> buildingB = campusBuildingRepository.findByName(placeB);

        if (buildingA.isEmpty() || buildingB.isEmpty()) {
            return false;
        }

        UUID idA = buildingA.get().getId();
        UUID idB = buildingB.get().getId();

        return !campusPathRepository.findByFromBuildingIdAndToBuildingId(idA, idB).isEmpty()
                || !campusPathRepository.findByFromBuildingIdAndToBuildingId(idB, idA).isEmpty();
    }

    private SlotEntity findSlotOrThrow(UUID slotId) {
        return slotRepository.findById(slotId)
                .orElseThrow(MatchingException.SLOT_NOT_FOUND::toException);
    }

    private void verifyOwnership(SlotEntity slot, UUID userId) {
        if (!slot.getUserId().equals(userId)) {
            throw MatchingException.SLOT_NOT_OWNED.toException();
        }
    }
}
