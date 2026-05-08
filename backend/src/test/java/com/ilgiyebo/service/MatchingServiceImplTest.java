package com.ilgiyebo.service;

import com.ilgiyebo.domain.campus.entity.CampusBuildingEntity;
import com.ilgiyebo.domain.campus.entity.CampusPathEntity;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.MatchEntity;
import com.ilgiyebo.domain.MissionEntity;
import com.ilgiyebo.domain.ScheduleEntity;
import com.ilgiyebo.domain.SlotEntity;
import com.ilgiyebo.domain.SlotStatus;
import com.ilgiyebo.domain.UserEntity;
import com.ilgiyebo.dto.BatchMatchingResultDto;
import com.ilgiyebo.dto.OverlapLocationDto;
import com.ilgiyebo.dto.RouteOverlapDto;
import com.ilgiyebo.domain.campus.repository.CampusBuildingRepository;
import com.ilgiyebo.domain.campus.repository.CampusPathRepository;
import com.ilgiyebo.domain.campus.repository.PlaceRepository;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceImplTest {

    @Mock private SlotRepository slotRepository;
    @Mock private UserRepository userRepository;
    @Mock private ScheduleRepository scheduleRepository;
    @Mock private BlockRepository blockRepository;
    @Mock private CampusBuildingRepository campusBuildingRepository;
    @Mock private CampusPathRepository campusPathRepository;
    @Mock private PlaceRepository placeRepository;
    @Mock private MatchRepository matchRepository;
    @Mock private MissionRepository missionRepository;
    @Mock private InteractionRepository interactionRepository;

    private MatchingServiceImpl matchingService;

    private final UUID userA = UUID.randomUUID();
    private final UUID userB = UUID.randomUUID();
    private UserEntity userEntityA;
    private UserEntity userEntityB;

    @BeforeEach
    void setUp() {
        userEntityA = UserEntity.builder().id(userA).build();
        userEntityB = UserEntity.builder().id(userB).build();
        matchingService = new MatchingServiceImpl(
                slotRepository, userRepository, scheduleRepository,
                blockRepository, campusBuildingRepository, campusPathRepository,
                placeRepository, matchRepository, missionRepository,
                interactionRepository);
    }

    // --- calculateRouteOverlap ---

    @Test
    void calculateRouteOverlap_samePlace_sameTime_returnsOverlap() {
        ScheduleEntity sa = schedule(userA, DayOfWeek.MONDAY, "09:00", "10:30", "공학관");
        ScheduleEntity sb = schedule(userB, DayOfWeek.MONDAY, "09:30", "11:00", "공학관");

        when(scheduleRepository.findAllByUserId(userA)).thenReturn(List.of(sa));
        when(scheduleRepository.findAllByUserId(userB)).thenReturn(List.of(sb));

        RouteOverlapDto result = matchingService.calculateRouteOverlap(userA, userB);

        assertTrue(result.hasOverlap());
        assertEquals(1, result.overlappingLocations().size());
        OverlapLocationDto loc = result.overlappingLocations().get(0);
        assertEquals("공학관", loc.fromBuilding());
        assertEquals("공학관", loc.toBuilding());
        assertTrue(loc.timeRange().contains("09:30"));
        assertTrue(loc.timeRange().contains("10:30"));
    }

    @Test
    void calculateRouteOverlap_samePlace_noTimeOverlap_returnsEmpty() {
        ScheduleEntity sa = schedule(userA, DayOfWeek.MONDAY, "09:00", "10:00", "공학관");
        ScheduleEntity sb = schedule(userB, DayOfWeek.MONDAY, "10:00", "11:00", "공학관");

        when(scheduleRepository.findAllByUserId(userA)).thenReturn(List.of(sa));
        when(scheduleRepository.findAllByUserId(userB)).thenReturn(List.of(sb));

        RouteOverlapDto result = matchingService.calculateRouteOverlap(userA, userB);

        assertFalse(result.hasOverlap());
        assertTrue(result.overlappingLocations().isEmpty());
    }

    @Test
    void calculateRouteOverlap_differentDay_returnsEmpty() {
        ScheduleEntity sa = schedule(userA, DayOfWeek.MONDAY, "09:00", "10:30", "공학관");
        ScheduleEntity sb = schedule(userB, DayOfWeek.TUESDAY, "09:00", "10:30", "공학관");

        when(scheduleRepository.findAllByUserId(userA)).thenReturn(List.of(sa));
        when(scheduleRepository.findAllByUserId(userB)).thenReturn(List.of(sb));

        RouteOverlapDto result = matchingService.calculateRouteOverlap(userA, userB);

        assertFalse(result.hasOverlap());
    }

    @Test
    void calculateRouteOverlap_adjacentBuildings_returnsOverlap() {
        ScheduleEntity sa = schedule(userA, DayOfWeek.WEDNESDAY, "13:00", "14:30", "공학관");
        ScheduleEntity sb = schedule(userB, DayOfWeek.WEDNESDAY, "13:30", "15:00", "과학관");

        UUID buildingIdA = UUID.randomUUID();
        UUID buildingIdB = UUID.randomUUID();

        CampusBuildingEntity buildingA = CampusBuildingEntity.builder()
                .id(buildingIdA).name("공학관").build();
        CampusBuildingEntity buildingB = CampusBuildingEntity.builder()
                .id(buildingIdB).name("과학관").build();

        when(scheduleRepository.findAllByUserId(userA)).thenReturn(List.of(sa));
        when(scheduleRepository.findAllByUserId(userB)).thenReturn(List.of(sb));
        when(campusBuildingRepository.findByName("공학관")).thenReturn(Optional.of(buildingA));
        when(campusBuildingRepository.findByName("과학관")).thenReturn(Optional.of(buildingB));
        when(campusPathRepository.findByFromBuildingIdAndToBuildingId(buildingIdA, buildingIdB))
                .thenReturn(List.of(CampusPathEntity.builder()
                        .fromBuilding(buildingA).toBuilding(buildingB)
                        .build()));

        RouteOverlapDto result = matchingService.calculateRouteOverlap(userA, userB);

        assertTrue(result.hasOverlap());
        assertEquals(1, result.overlappingLocations().size());
        assertEquals("공학관", result.overlappingLocations().get(0).fromBuilding());
        assertEquals("과학관", result.overlappingLocations().get(0).toBuilding());
    }

    @Test
    void calculateRouteOverlap_differentBuildings_notAdjacent_returnsEmpty() {
        ScheduleEntity sa = schedule(userA, DayOfWeek.MONDAY, "09:00", "10:30", "공학관");
        ScheduleEntity sb = schedule(userB, DayOfWeek.MONDAY, "09:00", "10:30", "도서관");

        when(scheduleRepository.findAllByUserId(userA)).thenReturn(List.of(sa));
        when(scheduleRepository.findAllByUserId(userB)).thenReturn(List.of(sb));
        when(campusBuildingRepository.findByName("공학관")).thenReturn(Optional.empty());

        RouteOverlapDto result = matchingService.calculateRouteOverlap(userA, userB);

        assertFalse(result.hasOverlap());
    }

    @Test
    void calculateRouteOverlap_emptyScheduleA_returnsEmpty() {
        when(scheduleRepository.findAllByUserId(userA)).thenReturn(List.of());
        when(scheduleRepository.findAllByUserId(userB)).thenReturn(List.of(
                schedule(userB, DayOfWeek.MONDAY, "09:00", "10:00", "공학관")));

        RouteOverlapDto result = matchingService.calculateRouteOverlap(userA, userB);

        assertFalse(result.hasOverlap());
        assertTrue(result.overlappingLocations().isEmpty());
    }

    @Test
    void calculateRouteOverlap_multipleOverlaps_returnsAll() {
        ScheduleEntity sa1 = schedule(userA, DayOfWeek.MONDAY, "09:00", "10:30", "공학관");
        ScheduleEntity sa2 = schedule(userA, DayOfWeek.WEDNESDAY, "14:00", "15:30", "도서관");
        ScheduleEntity sb1 = schedule(userB, DayOfWeek.MONDAY, "10:00", "11:30", "공학관");
        ScheduleEntity sb2 = schedule(userB, DayOfWeek.WEDNESDAY, "14:30", "16:00", "도서관");

        when(scheduleRepository.findAllByUserId(userA)).thenReturn(List.of(sa1, sa2));
        when(scheduleRepository.findAllByUserId(userB)).thenReturn(List.of(sb1, sb2));

        RouteOverlapDto result = matchingService.calculateRouteOverlap(userA, userB);

        assertTrue(result.hasOverlap());
        assertEquals(2, result.overlappingLocations().size());
    }

    // --- isBlocked ---

    @Test
    void isBlocked_aBlockedB_returnsTrue() {
        when(blockRepository.existsByUserIdAndBlockedUserId(userA, userB)).thenReturn(true);

        assertTrue(matchingService.isBlocked(userA, userB));
    }

    @Test
    void isBlocked_bBlockedA_returnsTrue() {
        when(blockRepository.existsByUserIdAndBlockedUserId(userA, userB)).thenReturn(false);
        when(blockRepository.existsByUserIdAndBlockedUserId(userB, userA)).thenReturn(true);

        assertTrue(matchingService.isBlocked(userA, userB));
    }

    @Test
    void isBlocked_noBlock_returnsFalse() {
        when(blockRepository.existsByUserIdAndBlockedUserId(userA, userB)).thenReturn(false);
        when(blockRepository.existsByUserIdAndBlockedUserId(userB, userA)).thenReturn(false);

        assertFalse(matchingService.isBlocked(userA, userB));
    }

    // --- executeBatchMatching ---

    @Test
    void executeBatchMatching_noEmptySlots_returnsZeroMatches() {
        when(slotRepository.findByStatus(SlotStatus.EMPTY)).thenReturn(List.of());

        BatchMatchingResultDto result = matchingService.executeBatchMatching();

        assertEquals(0, result.totalProcessed());
        assertEquals(0, result.matchesCreated());
    }

    @Test
    void executeBatchMatching_twoUsersWithOverlap_createsMatch() {
        SlotEntity slotA = SlotEntity.builder()
                .id(UUID.randomUUID()).user(userEntityA).status(SlotStatus.EMPTY).build();
        SlotEntity slotB = SlotEntity.builder()
                .id(UUID.randomUUID()).user(userEntityB).status(SlotStatus.EMPTY).build();

        when(slotRepository.findByStatus(SlotStatus.EMPTY)).thenReturn(List.of(slotA, slotB));

        ScheduleEntity sa = schedule(userA, DayOfWeek.MONDAY, "09:00", "10:30", "공학관");
        ScheduleEntity sb = schedule(userB, DayOfWeek.MONDAY, "09:30", "11:00", "공학관");
        when(scheduleRepository.findAllByUserId(userA)).thenReturn(List.of(sa));
        when(scheduleRepository.findAllByUserId(userB)).thenReturn(List.of(sb));

        when(blockRepository.existsByUserIdAndBlockedUserId(userA, userB)).thenReturn(false);
        when(blockRepository.existsByUserIdAndBlockedUserId(userB, userA)).thenReturn(false);

        when(campusBuildingRepository.findByName("공학관")).thenReturn(Optional.empty());

        when(matchRepository.save(org.mockito.ArgumentMatchers.any(MatchEntity.class)))
                .thenAnswer(inv -> {
                    MatchEntity m = inv.getArgument(0);
                    return m.toBuilder().id(UUID.randomUUID()).build();
                });
        when(interactionRepository.save(org.mockito.ArgumentMatchers.any(InteractionEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(missionRepository.save(org.mockito.ArgumentMatchers.any(MissionEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(slotRepository.save(org.mockito.ArgumentMatchers.any(SlotEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        BatchMatchingResultDto result = matchingService.executeBatchMatching();

        assertEquals(2, result.totalProcessed());
        assertEquals(1, result.matchesCreated());
    }

    @Test
    void executeBatchMatching_blockedUsers_doesNotMatch() {
        SlotEntity slotA = SlotEntity.builder()
                .id(UUID.randomUUID()).user(userEntityA).status(SlotStatus.EMPTY).build();
        SlotEntity slotB = SlotEntity.builder()
                .id(UUID.randomUUID()).user(userEntityB).status(SlotStatus.EMPTY).build();

        when(slotRepository.findByStatus(SlotStatus.EMPTY)).thenReturn(List.of(slotA, slotB));

        ScheduleEntity sa = schedule(userA, DayOfWeek.MONDAY, "09:00", "10:30", "공학관");
        ScheduleEntity sb = schedule(userB, DayOfWeek.MONDAY, "09:30", "11:00", "공학관");
        when(scheduleRepository.findAllByUserId(userA)).thenReturn(List.of(sa));
        when(scheduleRepository.findAllByUserId(userB)).thenReturn(List.of(sb));

        // A가 B를 차단
        when(blockRepository.existsByUserIdAndBlockedUserId(userA, userB)).thenReturn(true);

        BatchMatchingResultDto result = matchingService.executeBatchMatching();

        assertEquals(2, result.totalProcessed());
        assertEquals(0, result.matchesCreated());
    }

    @Test
    void executeBatchMatching_noOverlap_doesNotMatch() {
        SlotEntity slotA = SlotEntity.builder()
                .id(UUID.randomUUID()).user(userEntityA).status(SlotStatus.EMPTY).build();
        SlotEntity slotB = SlotEntity.builder()
                .id(UUID.randomUUID()).user(userEntityB).status(SlotStatus.EMPTY).build();

        when(slotRepository.findByStatus(SlotStatus.EMPTY)).thenReturn(List.of(slotA, slotB));

        // 다른 요일 → 겹침 없음
        ScheduleEntity sa = schedule(userA, DayOfWeek.MONDAY, "09:00", "10:30", "공학관");
        ScheduleEntity sb = schedule(userB, DayOfWeek.TUESDAY, "09:00", "10:30", "공학관");
        when(scheduleRepository.findAllByUserId(userA)).thenReturn(List.of(sa));
        when(scheduleRepository.findAllByUserId(userB)).thenReturn(List.of(sb));

        when(blockRepository.existsByUserIdAndBlockedUserId(userA, userB)).thenReturn(false);
        when(blockRepository.existsByUserIdAndBlockedUserId(userB, userA)).thenReturn(false);

        BatchMatchingResultDto result = matchingService.executeBatchMatching();

        assertEquals(2, result.totalProcessed());
        assertEquals(0, result.matchesCreated());
    }

    @Test
    void executeBatchMatching_userWithNoSchedule_excluded() {
        SlotEntity slotA = SlotEntity.builder()
                .id(UUID.randomUUID()).user(userEntityA).status(SlotStatus.EMPTY).build();
        SlotEntity slotB = SlotEntity.builder()
                .id(UUID.randomUUID()).user(userEntityB).status(SlotStatus.EMPTY).build();

        when(slotRepository.findByStatus(SlotStatus.EMPTY)).thenReturn(List.of(slotA, slotB));

        // userA에 시간표 없음
        when(scheduleRepository.findAllByUserId(userA)).thenReturn(List.of());
        when(scheduleRepository.findAllByUserId(userB)).thenReturn(List.of(
                schedule(userB, DayOfWeek.MONDAY, "09:00", "10:30", "공학관")));

        BatchMatchingResultDto result = matchingService.executeBatchMatching();

        // 시간표가 있는 사용자가 2명 미만이므로 매칭 불가
        assertEquals(2, result.totalProcessed());
        assertEquals(0, result.matchesCreated());
    }

    // --- helper ---

    private ScheduleEntity schedule(UUID userId, DayOfWeek day, String start, String end, String place) {
        return ScheduleEntity.builder()
                .userId(userId)
                .dayOfWeek(day)
                .startedAt(LocalTime.parse(start))
                .endedAt(LocalTime.parse(end))
                .place(place)
                .name("수업")
                .build();
    }
}
