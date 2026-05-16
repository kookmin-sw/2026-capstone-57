package com.ilgiyebo.domain.mission.service;

import com.ilgiyebo.domain.mission.dto.MissionDto;

import java.util.UUID;

/**
 * 미션 서비스 인터페이스.
 * 4단계(미션 기반 만남) 상호작용을 관리한다.
 */
public interface MissionService {

    /**
     * 미션 생성 요청 (SQS 발행 → AI 서버에서 RAG 기반 생성).
     * 매칭 성사 시 호출되며, 각 유저의 시간표에서 이동 구간을 파악하고
     * 캠퍼스 그래프에서 동선을 선택하여 미션 요청 큐에 발행한다.
     */
    void requestMissionGeneration(UUID matchId);

    /**
     * 미션 조회 (매칭 ID 기반).
     */
    MissionDto getMission(UUID matchId, UUID userId);

    /**
     * 미션 수행 확인.
     * 양쪽 사용자 모두 확인 시 5단계(회고) 해금.
     */
    MissionDto confirmMission(UUID matchId, UUID userId);
}
