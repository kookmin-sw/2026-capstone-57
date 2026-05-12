package com.ilgiyebo.domain.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ilgiyebo.domain.planner.service.PlannerService;
import com.ilgiyebo.domain.user.entity.ScheduleEntity;
import com.ilgiyebo.domain.user.entity.SemesterEntity;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.campus.entity.CampusBuildingEntity;
import com.ilgiyebo.domain.campus.repository.CampusBuildingRepository;
import com.ilgiyebo.domain.user.exception.UserException;
import com.ilgiyebo.domain.user.dto.ScheduleResponse;
import com.ilgiyebo.domain.user.dto.ScheduleUpsertResponse;
import com.ilgiyebo.domain.user.repository.ScheduleRepository;
import com.ilgiyebo.domain.user.repository.SemesterRepository;
import com.ilgiyebo.domain.user.repository.UserRepository;
import kong.unirest.core.ContentType;
import kong.unirest.core.Unirest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final XmlMapper xmlMapper = new XmlMapper();
    private static final Pattern FLOOR_PATTERN = Pattern.compile("(지하)?(\\d+)층");

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final SemesterRepository semesterRepository;
    private final CampusBuildingRepository campusBuildingRepository;
    private final PlannerService plannerService;

    @Value("${everytime.user-agent:EverytimeApp}")
    private String userAgent;

    @Transactional(readOnly = true)
    public ScheduleResponse getMySchedule(UUID userId) {
        validateUserExists(userId);
        SemesterEntity currentSemester = getCurrentSemester();
        List<ScheduleEntity> schedules = scheduleRepository.findAllByUserIdAndSemesterId(
                userId, currentSemester.getId());
        return ScheduleResponse.from(schedules);
    }

    @Transactional
    @SneakyThrows(IOException.class)
    public ScheduleUpsertResponse upsertMySchedule(UUID userId, String identifier) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(UserException.USER_NOT_FOUND::toException);

        // 현재 활성 학기 조회
        SemesterEntity currentSemester = getCurrentSemester();

        // 연관된 plan_entry 먼저 삭제 (FK 제약 해소)
        plannerService.deleteScheduleLinkedEntries(userId);

        // 해당 user + semester 기존 시간표 전체 삭제
        scheduleRepository.deleteByUserIdAndSemesterId(userId, currentSemester.getId());

        // 건물 이름 목록 조회 (이름 길이 내림차순 정렬)
        List<CampusBuildingEntity> buildings = campusBuildingRepository.findAll();
        buildings.sort(Comparator.comparingInt((CampusBuildingEntity b) -> b.getName().length()).reversed());

        byte[] response = Unirest.post("https://api.everytime.kr/find/timetable/table/friend")
                .contentType(ContentType.APPLICATION_FORM_URLENCODED.getMimeType())
                .header("User-Agent", userAgent)
                .field("identifier", identifier)
                .asBytes()
                .getBody();

        JsonNode root = xmlMapper.readTree(response);
        JsonNode table = root.get("table");
        JsonNode subjects = table != null ? table.get("subject") : null;

        if (subjects == null) {
            var emptyResult = plannerService.regenerateScheduleAutoEntries(userId);
            return ScheduleUpsertResponse.from(List.of(), emptyResult);
        }

        List<ScheduleEntity> schedules = new ArrayList<>();
        for (JsonNode subject : subjects) {
            JsonNode name = subject.get("name");

            JsonNode time = subject.get("time");
            if (time == null) continue;
            JsonNode data = time.get("data");
            if (data == null) continue;

            if (data.getNodeType().equals(JsonNodeType.ARRAY)) {
                for (JsonNode item : data) {
                    ScheduleEntity entity = ScheduleEntity.fromEverytime(name, item, user, currentSemester);
                    parsePlaceAndSet(entity, buildings);
                    scheduleRepository.save(entity);
                    schedules.add(entity);
                }
            } else {
                ScheduleEntity entity = ScheduleEntity.fromEverytime(name, data, user, currentSemester);
                parsePlaceAndSet(entity, buildings);
                scheduleRepository.save(entity);
                schedules.add(entity);
            }
        }

        // 시간표 등록 후 SCHEDULE_AUTO PLAN_ENTRY 재생성
        var plannerResult = plannerService.regenerateScheduleAutoEntries(userId);

        return ScheduleUpsertResponse.from(schedules, plannerResult);
    }

    /**
     * 현재 날짜 기준 활성 학기를 조회한다.
     */
    private SemesterEntity getCurrentSemester() {
        return semesterRepository.findCurrentByDate(LocalDate.now())
                .orElseThrow(UserException.SEMESTER_NOT_FOUND::toException);
    }

    /**
     * place 원본 문자열을 파싱하여 campus_building_id와 floor를 설정한다.
     */
    private void parsePlaceAndSet(ScheduleEntity entity, List<CampusBuildingEntity> buildings) {
        String place = entity.getPlace();
        if (place == null || place.isBlank()) {
            return;
        }

        for (CampusBuildingEntity building : buildings) {
            if (place.startsWith(building.getName())) {
                entity.setCampusBuilding(building);

                String remaining = place.substring(building.getName().length());
                Matcher matcher = FLOOR_PATTERN.matcher(remaining);
                if (matcher.find()) {
                    int floorNumber = Integer.parseInt(matcher.group(2));
                    if (matcher.group(1) != null) {
                        floorNumber = -floorNumber;
                    }
                    entity.setFloor(floorNumber);
                }
                break;
            }
        }
    }

    private void validateUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw UserException.USER_NOT_FOUND.toException();
        }
    }
}
