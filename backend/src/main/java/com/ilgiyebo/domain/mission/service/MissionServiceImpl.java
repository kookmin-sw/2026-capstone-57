package com.ilgiyebo.domain.mission.service;

import com.ilgiyebo.domain.campus.entity.CampusBuildingEntity;
import com.ilgiyebo.domain.campus.entity.CampusBuildingPlaceEntity;
import com.ilgiyebo.domain.campus.entity.CampusPathEntity;
import com.ilgiyebo.domain.campus.entity.CampusPathVenueEntity;
import com.ilgiyebo.domain.campus.repository.CampusBuildingPlaceRepository;
import com.ilgiyebo.domain.campus.repository.CampusBuildingRepository;
import com.ilgiyebo.domain.campus.repository.CampusPathRepository;
import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import com.ilgiyebo.domain.mission.dto.MissionDto;
import com.ilgiyebo.domain.mission.dto.MissionGenerateRequestMessage;
import com.ilgiyebo.domain.mission.dto.MissionGenerateRequestMessage.BuildingNode;
import com.ilgiyebo.domain.mission.dto.MissionGenerateRequestMessage.UserRouteInfo;
import com.ilgiyebo.domain.mission.entity.MissionEntity;
import com.ilgiyebo.domain.mission.entity.MissionStatus;
import com.ilgiyebo.domain.mission.exception.MissionException;
import com.ilgiyebo.domain.mission.repository.MissionRepository;
import com.ilgiyebo.domain.user.entity.ScheduleEntity;
import com.ilgiyebo.domain.user.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 미션 서비스 구현체.
 * 4단계(미션 기반 만남) 상호작용을 관리한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionServiceImpl implements MissionService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final int ACTUAL_END_OFFSET_MINUTES = 15;

    private final MissionRepository missionRepository;
    private final MatchRepository matchRepository;
    private final InteractionRepository interactionRepository;
    private final ScheduleRepository scheduleRepository;
    private final CampusBuildingRepository campusBuildingRepository;
    private final CampusPathRepository campusPathRepository;
    private final CampusBuildingPlaceRepository placeRepository;
    private final MissionRequestPublisher missionRequestPublisher;

    @Override
    @Transactional
    public void requestMissionGeneration(UUID matchId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(MissionException.MATCH_NOT_FOUND::toException);

        UUID userAId = match.getUserA().getId();
        UUID userBId = match.getUserB().getId();

        // 각 유저의 시간표 조회
        List<ScheduleEntity> schedulesA = scheduleRepository.findAllByUserId(userAId);
        List<ScheduleEntity> schedulesB = scheduleRepository.findAllByUserId(userBId);

        if (schedulesA.isEmpty() || schedulesB.isEmpty()) {
            log.warn("시간표 데이터 부족으로 미션 생성 불가: matchId={}, userA schedules={}, userB schedules={}",
                    matchId, schedulesA.size(), schedulesB.size());
            throw MissionException.NO_SCHEDULE_DATA.toException();
        }

        // 이동 구간 추출 및 겹침 찾기
        Optional<RouteOverlapResult> overlapResult = findBestRouteOverlap(schedulesA, schedulesB);

        if (overlapResult.isEmpty()) {
            log.warn("동선 겹침이 없어 미션 생성 불가: matchId={}", matchId);
            throw MissionException.NO_ROUTE_OVERLAP.toException();
        }

        RouteOverlapResult result = overlapResult.get();

        // 각 유저별 동선에서 subNodeIds 구성
        UserRouteInfo userARoute = buildUserRoute(result.scheduleA(), result.nextScheduleA());
        UserRouteInfo userBRoute = buildUserRoute(result.scheduleB(), result.nextScheduleB());

        // SQS 미션 요청 발행
        MissionGenerateRequestMessage message = MissionGenerateRequestMessage.of(
                matchId.toString(),
                userAId.toString(),
                userBId.toString(),
                result.dayOfWeek().name(),
                result.timeSlot(),
                userARoute,
                userBRoute
        );

        missionRequestPublisher.requestMissionGeneration(message);
        log.info("미션 생성 SQS 요청 발행: matchId={}, dayOfWeek={}, timeSlot={}", matchId, result.dayOfWeek(), result.timeSlot());
    }

    @Override
    @Transactional(readOnly = true)
    public MissionDto getMission(UUID matchId, UUID userId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(MissionException.MATCH_NOT_FOUND::toException);
        validateUserInMatch(match, userId);

        MissionEntity mission = missionRepository.findByMatchId(matchId)
                .orElseThrow(MissionException.MISSION_NOT_FOUND::toException);

        return MissionDto.from(mission);
    }

    @Override
    @Transactional
    public MissionDto confirmMission(UUID matchId, UUID userId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(MissionException.MATCH_NOT_FOUND::toException);
        validateUserInMatch(match, userId);

        MissionEntity mission = missionRepository.findByMatchId(matchId)
                .orElseThrow(MissionException.MISSION_NOT_FOUND::toException);

        if (mission.getStatus() == MissionStatus.CONFIRMED) {
            throw MissionException.MISSION_ALREADY_COMPLETED.toException();
        }

        if (mission.getStatus() == MissionStatus.EXPIRED) {
            throw MissionException.MISSION_EXPIRED.toException();
        }

        // 이미 확인한 사용자인지 체크
        String userIdStr = userId.toString();
        List<String> confirmedBy = mission.getConfirmedBy();
        if (confirmedBy == null) {
            confirmedBy = new ArrayList<>();
        }

        if (confirmedBy.contains(userIdStr)) {
            throw MissionException.ALREADY_CONFIRMED.toException();
        }

        // 확인 추가
        confirmedBy = new ArrayList<>(confirmedBy);
        confirmedBy.add(userIdStr);
        mission.setConfirmedBy(confirmedBy);

        // 양쪽 모두 확인 시 미션 완료 → 5단계 해금
        if (confirmedBy.size() >= 2) {
            mission.setStatus(MissionStatus.CONFIRMED);
            advanceToReviewStage(matchId);
        }

        missionRepository.save(mission);
        log.info("미션 수행 확인: matchId={}, userId={}, confirmedCount={}", matchId, userId, confirmedBy.size());

        return MissionDto.from(mission);
    }

    // ===== Private helpers =====

    /**
     * 양쪽 사용자의 시간표에서 이동 구간이 겹치는 최적의 동선을 찾는다.
     * timeSlot 계산: 시간표 상 수업 종료 시간에서 15분을 빼서 실제 종료 시간을 구하고,
     * 그 시점부터 다음 수업 시작까지를 이동 시간으로 산정한다.
     */
    private Optional<RouteOverlapResult> findBestRouteOverlap(
            List<ScheduleEntity> schedulesA, List<ScheduleEntity> schedulesB) {

        // 요일별로 그룹핑
        Map<DayOfWeek, List<ScheduleEntity>> byDayA = schedulesA.stream()
                .collect(Collectors.groupingBy(ScheduleEntity::getDayOfWeek));
        Map<DayOfWeek, List<ScheduleEntity>> byDayB = schedulesB.stream()
                .collect(Collectors.groupingBy(ScheduleEntity::getDayOfWeek));

        for (DayOfWeek day : byDayA.keySet()) {
            List<ScheduleEntity> daySchedulesA = byDayA.get(day);
            List<ScheduleEntity> daySchedulesB = byDayB.get(day);
            if (daySchedulesB == null) continue;

            // 시간순 정렬
            daySchedulesA.sort(Comparator.comparing(ScheduleEntity::getStartedAt));
            daySchedulesB.sort(Comparator.comparing(ScheduleEntity::getStartedAt));

            // 각 유저의 연속 수업 쌍에서 이동 구간 추출
            List<TransitSegment> transitsA = extractTransitSegments(daySchedulesA);
            List<TransitSegment> transitsB = extractTransitSegments(daySchedulesB);

            // 이동 구간 간 시간대 겹침 찾기
            for (TransitSegment segA : transitsA) {
                for (TransitSegment segB : transitsB) {
                    if (hasTimeOverlap(segA, segB) && hasBuildingOverlap(segA, segB)) {
                        String timeSlot = segA.actualEnd().format(TIME_FMT) + "~" + segA.nextStart().format(TIME_FMT);
                        return Optional.of(new RouteOverlapResult(
                                segA.currentSchedule(), segA.nextSchedule(),
                                segB.currentSchedule(), segB.nextSchedule(),
                                day,
                                timeSlot
                        ));
                    }
                }
            }
        }

        return Optional.empty();
    }

    /**
     * 연속 수업 쌍에서 이동 구간(transit segment)을 추출한다.
     * 이동 구간: 현재 수업 실제 종료 시간 ~ 다음 수업 시작 시간
     */
    private List<TransitSegment> extractTransitSegments(List<ScheduleEntity> sortedSchedules) {
        List<TransitSegment> segments = new ArrayList<>();
        for (int i = 0; i < sortedSchedules.size() - 1; i++) {
            ScheduleEntity current = sortedSchedules.get(i);
            ScheduleEntity next = sortedSchedules.get(i + 1);

            if (current.getCampusBuilding() == null || next.getCampusBuilding() == null) {
                continue;
            }

            // 실제 종료 시간 = 시간표 종료 시간 - 15분
            LocalTime actualEnd = current.getEndedAt().minusMinutes(ACTUAL_END_OFFSET_MINUTES);
            LocalTime nextStart = next.getStartedAt();

            // 이동 시간이 존재하는 경우만 (실제 종료 < 다음 시작)
            if (actualEnd.isBefore(nextStart)) {
                segments.add(new TransitSegment(current, next, actualEnd, nextStart));
            }
        }
        return segments;
    }

    /**
     * 두 이동 구간의 시간대가 겹치는지 확인한다.
     */
    private boolean hasTimeOverlap(TransitSegment segA, TransitSegment segB) {
        return segA.actualEnd().isBefore(segB.nextStart()) && segB.actualEnd().isBefore(segA.nextStart());
    }

    /**
     * 두 이동 구간의 출발/도착 건물이 겹치는지 확인한다.
     * 같은 건물로 이동하거나, 같은 건물에서 출발하거나, 인접 건물인 경우 겹침으로 판정.
     */
    private boolean hasBuildingOverlap(TransitSegment segA, TransitSegment segB) {
        UUID fromA = segA.currentSchedule().getCampusBuilding().getId();
        UUID toA = segA.nextSchedule().getCampusBuilding().getId();
        UUID fromB = segB.currentSchedule().getCampusBuilding().getId();
        UUID toB = segB.nextSchedule().getCampusBuilding().getId();

        // 도착지가 같은 경우
        if (toA.equals(toB)) return true;
        // 출발지가 같은 경우
        if (fromA.equals(fromB)) return true;
        // 한쪽의 도착지가 다른 쪽의 출발지인 경우
        if (toA.equals(fromB) || toB.equals(fromA)) return true;
        // 인접 건물 (campus_path로 연결)
        if (areAdjacentBuildings(toA, toB)) return true;

        return false;
    }

    /**
     * 두 건물이 CampusPath로 연결된 인접 건물인지 확인한다.
     */
    private boolean areAdjacentBuildings(UUID buildingIdA, UUID buildingIdB) {
        return !campusPathRepository.findByFromBuildingIdAndToBuildingId(buildingIdA, buildingIdB).isEmpty()
                || !campusPathRepository.findByFromBuildingIdAndToBuildingId(buildingIdB, buildingIdA).isEmpty();
    }

    /**
     * 스케줄 엔티티 쌍으로부터 UserRouteInfo를 구성한다.
     * subNodeIds에는 동선의 venue ID + 출발/도착 건물의 place ID를 포함한다.
     */
    private UserRouteInfo buildUserRoute(ScheduleEntity current, ScheduleEntity next) {
        CampusBuildingEntity fromBuilding = current.getCampusBuilding();
        CampusBuildingEntity toBuilding = next.getCampusBuilding();

        BuildingNode fromNode = new BuildingNode(fromBuilding.getId().toString(), fromBuilding.getName());
        BuildingNode toNode = new BuildingNode(toBuilding.getId().toString(), toBuilding.getName());

        List<String> subNodeIds = new ArrayList<>();

        // 1. 동선(path)의 venue ID 추가
        List<CampusPathEntity> paths = campusPathRepository.findByFromBuildingIdAndToBuildingId(
                fromBuilding.getId(), toBuilding.getId());
        if (!paths.isEmpty()) {
            CampusPathEntity selectedPath = paths.get(0);
            if (selectedPath.getPathVenues() != null) {
                for (CampusPathVenueEntity pv : selectedPath.getPathVenues()) {
                    subNodeIds.add(pv.getVenue().getId().toString());
                }
            }
        }

        // 2. 출발 건물의 place ID 추가
        List<CampusBuildingPlaceEntity> fromPlaces = placeRepository.findByBuildingId(fromBuilding.getId());
        for (CampusBuildingPlaceEntity place : fromPlaces) {
            subNodeIds.add(place.getId().toString());
        }

        // 3. 도착 건물의 place ID 추가 (출발과 도착이 다른 경우)
        if (!fromBuilding.getId().equals(toBuilding.getId())) {
            List<CampusBuildingPlaceEntity> toPlaces = placeRepository.findByBuildingId(toBuilding.getId());
            for (CampusBuildingPlaceEntity place : toPlaces) {
                subNodeIds.add(place.getId().toString());
            }
        }

        return new UserRouteInfo(fromNode, toNode, subNodeIds);
    }

    /**
     * 미션 완료 시 5단계(회고)로 진행한다.
     */
    private void advanceToReviewStage(UUID matchId) {
        InteractionEntity interaction = interactionRepository.findByMatchId(matchId).orElse(null);
        if (interaction == null) {
            log.warn("미션 완료 후 상호작용을 찾을 수 없습니다: matchId={}", matchId);
            return;
        }

        if (interaction.getCurrentStage() == 4) {
            interaction.setCurrentStage(5);
            interaction.setStageStatus(StageStatus.IN_PROGRESS);
            interactionRepository.save(interaction);
            log.info("5단계(회고) 해금: matchId={}", matchId);
        }
    }

    private void validateUserInMatch(MatchEntity match, UUID userId) {
        if (!match.getUserA().getId().equals(userId) && !match.getUserB().getId().equals(userId)) {
            throw MissionException.USER_NOT_IN_MATCH.toException();
        }
    }

    // ===== Inner records =====

    private record TransitSegment(
        ScheduleEntity currentSchedule,
        ScheduleEntity nextSchedule,
        LocalTime actualEnd,
        LocalTime nextStart
    ) {}

    private record RouteOverlapResult(
        ScheduleEntity scheduleA,
        ScheduleEntity nextScheduleA,
        ScheduleEntity scheduleB,
        ScheduleEntity nextScheduleB,
        DayOfWeek dayOfWeek,
        String timeSlot
    ) {}
}
