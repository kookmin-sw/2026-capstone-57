package com.ilgiyebo.domain.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ilgiyebo.domain.ai.exception.BedrockInvocationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class BedrockClientService {

    private final BedrockRuntimeClient bedrockRuntimeClient;
    private final ObjectMapper objectMapper;

    @Value("${aws.bedrock.model-id:anthropic.claude-3-sonnet-20240229-v1:0}")
    private String modelId;

    @Value("${aws.bedrock.max-tokens:4096}")
    private int maxTokens;

    /**
     * Bedrock 모델 호출
     * @param prompt 완성된 프롬프트 문자열
     * @return AI 모델의 원시 응답 문자열
     * @throws BedrockInvocationException 모델 호출 실패 시
     */
    public String invokeModel(String prompt) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("anthropic_version", "bedrock-2023-05-31");
            requestBody.put("max_tokens", maxTokens);

            ArrayNode messages = requestBody.putArray("messages");
            ObjectNode message = messages.addObject();
            message.put("role", "user");
            message.put("content", prompt);

            String requestJson = objectMapper.writeValueAsString(requestBody);

            InvokeModelRequest request = InvokeModelRequest.builder()
                    .modelId(modelId)
                    .contentType("application/json")
                    .accept("application/json")
                    .body(SdkBytes.fromUtf8String(requestJson))
                    .build();

            InvokeModelResponse response = bedrockRuntimeClient.invokeModel(request);
            String responseBody = response.body().asUtf8String();

            JsonNode responseJson = objectMapper.readTree(responseBody);
            JsonNode content = responseJson.get("content");
            if (content != null && content.isArray() && !content.isEmpty()) {
                return content.get(0).get("text").asText();
            }

            throw new BedrockInvocationException("응답에서 텍스트를 추출할 수 없습니다", null);
        } catch (BedrockInvocationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Bedrock 모델 호출 실패: {}", e.getMessage(), e);
            throw new BedrockInvocationException(e.getMessage(), e);
        }
    }
}
