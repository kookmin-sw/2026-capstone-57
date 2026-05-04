package com.ilgiyebo.domain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ScheduleEntityTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("fromEverytime - 에브리타임 시간표 데이터를 ScheduleEntity로 변환한다")
    void fromEverytime_convertsCorrectly() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        // 에브리타임 subject.name 노드
        JsonNode nameNode = objectMapper.readTree("""
                { "value": "데이터베이스" }
                """);

        // 에브리타임 time.data 노드
        // day: 0 = 월요일, starttime: 114 = 9시30분 (114*5=570분=9h30m), endtime: 126 = 10시30분 (126*5=630분=10h30m)
        JsonNode dataNode = objectMapper.readTree("""
                {
                    "day": "0",
                    "starttime": "114",
                    "endtime": "126",
                    "place": "공학관 301호"
                }
                """);

        // when
        ScheduleEntity entity = ScheduleEntity.fromEverytime(nameNode, dataNode, userId);

        // then
        assertEquals("데이터베이스", entity.getName());
        assertEquals(DayOfWeek.MONDAY, entity.getDayOfWeek());
        assertEquals(LocalTime.of(9, 30, 0), entity.getStartedAt());
        assertEquals(LocalTime.of(10, 30, 0), entity.getEndedAt());
        assertEquals("공학관 301호", entity.getPlace());
        assertEquals(userId, entity.getUserId());
    }

    @Test
    @DisplayName("fromEverytime - 수요일 오후 수업을 올바르게 변환한다")
    void fromEverytime_wednesdayAfternoon() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        JsonNode nameNode = objectMapper.readTree("""
                { "value": "알고리즘" }
                """);

        // day: 2 = 수요일, starttime: 156 = 13시 (156*5=780분=13h0m), endtime: 174 = 14시30분 (174*5=870분=14h30m)
        JsonNode dataNode = objectMapper.readTree("""
                {
                    "day": "2",
                    "starttime": "156",
                    "endtime": "174",
                    "place": "IT관 201호"
                }
                """);

        // when
        ScheduleEntity entity = ScheduleEntity.fromEverytime(nameNode, dataNode, userId);

        // then
        assertEquals("알고리즘", entity.getName());
        assertEquals(DayOfWeek.WEDNESDAY, entity.getDayOfWeek());
        assertEquals(LocalTime.of(13, 0, 0), entity.getStartedAt());
        assertEquals(LocalTime.of(14, 30, 0), entity.getEndedAt());
        assertEquals("IT관 201호", entity.getPlace());
    }

    @Test
    @DisplayName("fromEverytime - 금요일 수업 (day=4)을 올바르게 변환한다")
    void fromEverytime_friday() throws Exception {
        // given
        UUID userId = UUID.randomUUID();

        JsonNode nameNode = objectMapper.readTree("""
                { "value": "캡스톤디자인" }
                """);

        // day: 4 = 금요일, starttime: 108 = 9시 (108*5=540분=9h0m), endtime: 144 = 12시 (144*5=720분=12h0m)
        JsonNode dataNode = objectMapper.readTree("""
                {
                    "day": "4",
                    "starttime": "108",
                    "endtime": "144",
                    "place": "공학관 세미나실"
                }
                """);

        // when
        ScheduleEntity entity = ScheduleEntity.fromEverytime(nameNode, dataNode, userId);

        // then
        assertEquals("캡스톤디자인", entity.getName());
        assertEquals(DayOfWeek.FRIDAY, entity.getDayOfWeek());
        assertEquals(LocalTime.of(9, 0, 0), entity.getStartedAt());
        assertEquals(LocalTime.of(12, 0, 0), entity.getEndedAt());
        assertEquals("공학관 세미나실", entity.getPlace());
        assertEquals(userId, entity.getUserId());
    }
}
