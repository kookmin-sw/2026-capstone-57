package com.ilgiyebo.domain.mission.service;

import com.ilgiyebo.common.config.AiServerProperties;
import com.ilgiyebo.domain.mission.dto.CampusNodeIndexRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 캠퍼스 노드 인덱싱 HTTP 클라이언트.
 * 장소 등록/수정 시 AI 서버의 POST /api/campus-nodes/index를 호출하여
 * ChromaDB campus_nodes 컬렉션과 동기화한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CampusNodeIndexClient {

    private final AiServerProperties aiServerProperties;
    private final ObjectMapper objectMapper;

    /**
     * AI 서버에 캠퍼스 노드 인덱싱을 요청한다.
     * AI 서버가 설정되지 않은 경우 (로컬 환경) 건너뛴다.
     */
    public void indexNode(CampusNodeIndexRequest request) {
        if (!aiServerProperties.isConfigured()) {
            log.debug("AI 서버가 설정되지 않아 캠퍼스 노드 인덱싱을 건너뜁니다: nodeId={}", request.nodeId());
            return;
        }

        String url = aiServerProperties.getBaseUrl() + "/api/campus-nodes/index";
        String json;
        try {
            json = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            log.error("캠퍼스 노드 인덱싱 요청 직렬화 실패: nodeId={}", request.nodeId(), e);
            return;
        }

        int maxRetries = aiServerProperties.getMaxRetries();
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                HttpResponse<String> response = Unirest.post(url)
                        .header("Content-Type", "application/json")
                        .body(json)
                        .asString();

                if (response.isSuccess()) {
                    log.info("캠퍼스 노드 인덱싱 성공: nodeId={}, source={}", request.nodeId(), request.source());
                    return;
                } else {
                    log.warn("캠퍼스 노드 인덱싱 실패 (시도 {}/{}): nodeId={}, status={}, body={}",
                            attempt, maxRetries, request.nodeId(), response.getStatus(), response.getBody());
                }
            } catch (Exception e) {
                log.warn("캠퍼스 노드 인덱싱 요청 중 에러 (시도 {}/{}): nodeId={}",
                        attempt, maxRetries, request.nodeId(), e);
            }
        }

        log.error("캠퍼스 노드 인덱싱 최종 실패 ({}회 재시도 후): nodeId={}", maxRetries, request.nodeId());
    }
}
