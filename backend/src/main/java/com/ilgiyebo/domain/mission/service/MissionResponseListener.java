package com.ilgiyebo.domain.mission.service;

import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import com.ilgiyebo.domain.mission.dto.MissionGenerateResponseMessage;
import com.ilgiyebo.domain.mission.entity.MissionEntity;
import com.ilgiyebo.domain.mission.entity.MissionStatus;
import com.ilgiyebo.domain.mission.repository.MissionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * AI 서버로부터 미션 생성 결과를 SQS 응답큐에서 수신하는 리스너.
 * MISSION_GENERATED 응답 수신 시 Mission 엔티티를 생성하여 DB에 저장한다.
 */
@Slf4j
@Component
@Profile("!local")
@RequiredArgsConstructor
public class MissionResponseListener {

    private final MissionRepository missionRepository;
    private final MatchRepository matchRepository;
    private final ObjectMapper objectMapper;

    @SqsListener("${cloud.aws.sqs.mission-response-queue}")
    @Transactional
    public void handleMissionResponse(String messageJson) {
        try {
            MissionGenerateResponseMessage response = objectMapper.readValue(
                    messageJson, MissionGenerateResponseMessage.class);

            if (!"MISSION_GENERATED".equals(response.action())) {
                log.debug("미션 응답이 아닌 메시지 무시: action={}", response.action());
                return;
            }

            if (!"SUCCESS".equals(response.status())) {
                log.warn("AI 미션 생성 실패: 매칭ID={}, 상태={}, 에러={}",
                        response.matchId(), response.status(), response.errorMessage());
                return;
            }

            if (response.mission() == null) {
                log.warn("AI 미션 응답에 미션 데이터가 없습니다: 매칭ID={}", response.matchId());
                return;
            }

            UUID matchId = UUID.fromString(response.matchId());

            // 이미 미션이 존재하는 경우 업데이트
            Optional<MissionEntity> existingMission = missionRepository.findByMatchId(matchId);
            if (existingMission.isPresent()) {
                MissionEntity mission = existingMission.get();
                mission.setLocation(response.mission().location());
                mission.setActivity(response.mission().activity());
                mission.setDescription(response.mission().description());
                mission.setSelectedNodeId(response.mission().selectedNodeId());
                mission.setDayOfWeek(response.mission().dayOfWeek());
                mission.setTimeSlot(response.mission().timeSlot());
                missionRepository.save(mission);
                log.info("기존 미션 업데이트 완료: 매칭ID={}, location={}", matchId, response.mission().location());
                return;
            }

            // 새 미션 생성
            MatchEntity match = matchRepository.findById(matchId).orElse(null);
            if (match == null) {
                log.warn("미션 응답에 해당하는 매칭을 찾을 수 없습니다: 매칭ID={}", matchId);
                return;
            }

            // 미션 기한: 매칭 주기 종료일(금요일) 23:59:59
            Instant deadline = match.getCycleEndDate().plusDays(1).atStartOfDay()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .minusSeconds(1);

            MissionEntity mission = MissionEntity.builder()
                    .match(match)
                    .location(response.mission().location())
                    .activity(response.mission().activity())
                    .description(response.mission().description())
                    .deadline(deadline)
                    .confirmedBy(List.of())
                    .status(MissionStatus.PENDING)
                    .dayOfWeek(response.mission().dayOfWeek())
                    .timeSlot(response.mission().timeSlot())
                    .selectedNodeId(response.mission().selectedNodeId())
                    .build();
            missionRepository.save(mission);

            log.info("AI 미션 응답 수신 및 DB 저장 완료: 매칭ID={}, location={}, activity={}",
                    matchId, response.mission().location(), response.mission().activity());

        } catch (Exception e) {
            log.error("AI 미션 응답 처리 중 서버 에러 발생: {}", messageJson, e);
        }
    }
}
