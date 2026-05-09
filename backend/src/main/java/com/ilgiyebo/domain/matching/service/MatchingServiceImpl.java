package com.ilgiyebo.domain.matching.service;

import com.ilgiyebo.domain.campus.entity.CampusBuildingEntity;
import com.ilgiyebo.domain.campus.entity.CampusBuildingPlaceEntity;
import com.ilgiyebo.domain.campus.entity.CampusPathEntity;
import com.ilgiyebo.domain.campus.repository.CampusBuildingRepository;
import com.ilgiyebo.domain.campus.repository.CampusPathRepository;
import com.ilgiyebo.domain.campus.repository.PlaceRepository;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.entity.MatchStatus;
import com.ilgiyebo.domain.mission.entity.MissionEntity;
import com.ilgiyebo.domain.mission.entity.MissionStatus;
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
import com.ilgiyebo.domain.mission.repository.MissionRepository;
import com.ilgiyebo.domain.user.repository.ScheduleRepository;
import com.ilgiyebo.domain.matching.repository.SlotRepository;
import com.ilgiyebo.domain.user.repository.UserRepository;
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
                .collect(Collectors.groupingBy(slot -> slot.getUser().getId()));

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
                        slotA.setCurrentMatch(match);
                        slotRepository.save(slotA);

                        slotB.setStatus(SlotStatus.ACTIVE);
                        slotB.setCurrentMatch(match);
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
    private MatchEntity createMatch(UUID userAId, UUID userBId, SlotEntity slotA, SlotEntity slotB,
                                    LocalDate cycleStart, LocalDate cycleEnd) {
        UserEntity userA = userRepository.findById(userAId)
                .orElseThrow(MatchingException.USER_NOT_FOUND::toException);
        UserEntity userB = userRepository.findById(userBId)
                .orElseThrow(MatchingException.USER_NOT_FOUND::toException);

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
        return matchRepository.save(match);
    }

    /**
     * 매칭에 대한 상호작용 엔티티를 생성한다.
     */
    private void createInteraction(UUID matchId) {
        MatchEntity matchRef = matchRepository.getReferenceById(matchId);
        InteractionEntity interaction = InteractionEntity.builder()
                .match(matchRef)
                .currentStage(1)
                .stageStatus(StageStatus.IN_PROGRESS)
                .quizCompletedBy(List.of())
                .missionConfirmedBy(List.of())
                .reviewCompletedBy(List.of())
                .build();
        interactionRepository.save(interaction);
    }

    /**
     * 선택된 동선 겹침 정보를 기반으로 미션 데이터를 생성한다.
     *
     * 1. 이동 중 만나는 경우 (from != to): campus_path의 venue를 사용
     * 2. 같은 건물에 머무르는 경우 (from == to): place를 사용
     */
    private void createMissionFromOverlap(UUID matchId, OverlapLocationDto overlap, LocalDate cycleEnd) {
        String location;
        String activity;
        String description;

        boolean sameBuilding = overlap.fromBuilding().equals(overlap.toBuilding());

        if (sameBuilding) {
            // 같은 건물에 머무르는 경우 → place 사용
            location = overlap.fromBuilding();
            activity = "만남";
            description = overlap.fromBuilding() + "에서 " + overlap.timeRange() + " 시간대에 만남";

            Optional<CampusBuildingEntity> building = campusBuildingRepository.findByName(overlap.fromBuilding());
            if (building.isPresent()) {
                List<CampusBuildingPlaceEntity> places = placeRepository.findByBuildingId(building.get().getId());
                if (!places.isEmpty()) {
                    CampusBuildingPlaceEntity selectedPlace = places.get(0);
                    location = overlap.fromBuilding() + " " + selectedPlace.getFloor() + "층 " + selectedPlace.getName();
                    activity = location + "에서 만나기";
                    description = location + "에서 " + overlap.timeRange() + " 시간대에 만남";
                }
            }
        } else {
            // 이동 중 만나는 경우 → campus_path의 venue 사용
            location = overlap.fromBuilding() + " → " + overlap.toBuilding();
            activity = "이동 중 만남";
            description = overlap.fromBuilding() + "에서 " + overlap.toBuilding() + "으로 이동 중 " + overlap.timeRange() + " 시간대에 만남";

            Optional<CampusBuildingEntity> fromBuilding = campusBuildingRepository.findByName(overlap.fromBuilding());
            Optional<CampusBuildingEntity> toBuilding = campusBuildingRepository.findByName(overlap.toBuilding());

            if (fromBuilding.isPresent() && toBuilding.isPresent()) {
                List<CampusPathEntity> paths = campusPathRepository.findByFromBuildingIdAndToBuildingId(
                        fromBuilding.get().getId(), toBuilding.get().getId());
                if (!paths.isEmpty()) {
                    CampusPathEntity selectedPath = paths.get(0);
                    String venueName = selectedPath.getVenue().getName();
                    location = venueName;
                    activity = venueName + "에서 만나기";
                    description = overlap.fromBuilding() + " → " + overlap.toBuilding() + " 이동 중 " + venueName + "에서 " + overlap.timeRange() + " 시간대에 만남";
                }
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
                .description(description)
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
