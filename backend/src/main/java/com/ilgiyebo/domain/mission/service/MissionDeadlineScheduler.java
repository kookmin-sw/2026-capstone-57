package com.ilgiyebo.domain.mission.service;

import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.entity.TerminationReason;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.matching.entity.MatchStatus;
import com.ilgiyebo.domain.matching.entity.SlotEntity;
import com.ilgiyebo.domain.matching.entity.SlotStatus;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import com.ilgiyebo.domain.matching.repository.SlotRepository;
import com.ilgiyebo.domain.mission.entity.MissionEntity;
import com.ilgiyebo.domain.mission.entity.MissionStatus;
import com.ilgiyebo.domain.mission.repository.MissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * 미션 기한 만료 처리 스케줄러.
 * 매 시간 실행되어 기한이 지난 미션을 처리한다.
 * 기한 만료 시 매칭을 종료한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MissionDeadlineScheduler {

    private final MissionRepository missionRepository;
    private final MatchRepository matchRepository;
    private final InteractionRepository interactionRepository;
    private final SlotRepository slotRepository;

    /**
     * 매 시간 미션 기한 만료를 확인한다.
     * 기한이 지난 PENDING 미션은 EXPIRED 처리하고 매칭을 종료한다.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void checkMissionDeadlines() {
        Instant now = Instant.now();
        List<MissionEntity> expiredMissions = missionRepository.findByStatusAndDeadlineBefore(
                MissionStatus.PENDING, now);

        for (MissionEntity mission : expiredMissions) {
            handleMissionExpiry(mission);
        }

        if (!expiredMissions.isEmpty()) {
            log.info("미션 기한 만료 처리 완료: {}건", expiredMissions.size());
        }
    }

    /**
     * 기한 만료된 미션에 대해 매칭을 종료한다.
     */
    private void handleMissionExpiry(MissionEntity mission) {
        mission.setStatus(MissionStatus.EXPIRED);
        missionRepository.save(mission);

        MatchEntity match = mission.getMatch();
        match.setStatus(MatchStatus.TERMINATED);
        matchRepository.save(match);

        // 상호작용 종료
        InteractionEntity interaction = interactionRepository.findByMatchId(match.getId()).orElse(null);
        if (interaction != null) {
            interaction.setStageStatus(StageStatus.TERMINATED);
            interaction.setTerminationReason(TerminationReason.EXPIRED.name());
            interactionRepository.save(interaction);
        }

        // 슬롯 초기화
        SlotEntity slotA = match.getSlotA();
        SlotEntity slotB = match.getSlotB();
        if (slotA != null) {
            slotA.setStatus(SlotStatus.EMPTY);
            slotA.setCurrentMatch(null);
            slotRepository.save(slotA);
        }
        if (slotB != null) {
            slotB.setStatus(SlotStatus.EMPTY);
            slotB.setCurrentMatch(null);
            slotRepository.save(slotB);
        }

        log.info("미션 기한 만료로 매칭 종료: matchId={}, missionId={}",
                match.getId(), mission.getId());
    }
}
