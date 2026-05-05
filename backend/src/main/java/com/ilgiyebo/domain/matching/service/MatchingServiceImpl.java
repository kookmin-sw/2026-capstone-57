package com.ilgiyebo.service;

import com.ilgiyebo.domain.CampusBuildingEntity;
import com.ilgiyebo.domain.ScheduleEntity;
import com.ilgiyebo.domain.SlotEntity;
import com.ilgiyebo.domain.SlotPriority;
import com.ilgiyebo.domain.SlotStatus;
import com.ilgiyebo.domain.matching.exception.MatchingException;
import com.ilgiyebo.dto.OverlapLocationDto;
import com.ilgiyebo.dto.RouteOverlapDto;
import com.ilgiyebo.dto.SlotResponseDto;
import com.ilgiyebo.repository.BlockRepository;
import com.ilgiyebo.repository.CampusBuildingRepository;
import com.ilgiyebo.repository.CampusPathRepository;
import com.ilgiyebo.repository.ScheduleRepository;
import com.ilgiyebo.repository.SlotRepository;
import com.ilgiyebo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

    @Override
    @Transactional(readOnly = true)
    public List<SlotResponseDto> getSlots(UUID userId) {
        return slotRepository.findByUserId(userId).stream()
                .map(SlotResponseDto::from)
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

        return campusPathRepository.findByFromBuildingIdAndToBuildingId(idA, idB).isPresent()
                || campusPathRepository.findByFromBuildingIdAndToBuildingId(idB, idA).isPresent();
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
