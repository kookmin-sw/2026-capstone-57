package com.ilgiyebo.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.ilgiyebo.domain.ScheduleEntity;
import com.ilgiyebo.domain.user.exception.UserException;
import com.ilgiyebo.dto.ScheduleResponse;
import com.ilgiyebo.repository.ScheduleRepository;
import com.ilgiyebo.repository.UserRepository;
import kong.unirest.core.ContentType;
import kong.unirest.core.Unirest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduleService {

    private static final XmlMapper xmlMapper = new XmlMapper();
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    @Value("${everytime.user-agent:EverytimeApp}")
    private String userAgent;

    @Transactional(readOnly = true)
    public ScheduleResponse getMySchedule(UUID userId) {
        validateUserExists(userId);
        List<ScheduleEntity> schedules = scheduleRepository.findAllByUserId(userId);
        return ScheduleResponse.from(schedules);
    }

    @Transactional
    @SneakyThrows(IOException.class)
    public ScheduleResponse upsertMySchedule(UUID userId, String identifier) {
        validateUserExists(userId);
        scheduleRepository.deleteByUserId(userId);

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
            return ScheduleResponse.from(List.of());
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
                    ScheduleEntity entity = ScheduleEntity.fromEverytime(name, item, userId);
                    scheduleRepository.save(entity);
                    schedules.add(entity);
                }
            } else {
                ScheduleEntity entity = ScheduleEntity.fromEverytime(name, data, userId);
                scheduleRepository.save(entity);
                schedules.add(entity);
            }
        }

        return ScheduleResponse.from(schedules);
    }

    private void validateUserExists(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw UserException.USER_NOT_FOUND.toException();
        }
    }
}
