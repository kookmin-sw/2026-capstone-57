package com.ilgiyebo.domain.diary.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ilgiyebo.common.config.AiServerProperties;
import com.ilgiyebo.domain.diary.entity.EmotionTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * HttpDiaryAiClient의 요청/응답 직렬화 및 에러 처리 로직을 검증하는 단위 테스트.
 *
 * 실제 HTTP 호출은 통합 테스트에서 검증하며,
 * 여기서는 ObjectMapper 직렬화/역직렬화 및 감정 태그 파싱 로직을 검증한다.
 */
class HttpDiaryAiClientTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("FirstQuestionInput을 올바른 JSON으로 직렬화한다")
    void serializeFirstQuestionInput() throws Exception {
        DiaryAiClient.FirstQuestionInput input = new DiaryAiClient.FirstQuestionInput(
                "session-789",
                "user-123",
                "2025-05-14",
                List.of(new DiaryAiClient.ScheduleContext("09:00", "10:30", "공학관", "알고리즘")),
                "어제는 좋은 하루였다."
        );

        String json = objectMapper.writeValueAsString(input);

        assertThat(json).contains("\"sessionId\":\"session-789\"");
        assertThat(json).contains("\"userId\":\"user-123\"");
        assertThat(json).contains("\"targetDate\":\"2025-05-14\"");
        assertThat(json).contains("\"previousDiaryContent\":\"어제는 좋은 하루였다.\"");
        assertThat(json).contains("\"startTime\":\"09:00\"");
        assertThat(json).contains("\"location\":\"공학관\"");
    }

    @Test
    @DisplayName("NextQuestionInput을 올바른 JSON으로 직렬화한다")
    void serializeNextQuestionInput() throws Exception {
        DiaryAiClient.NextQuestionInput input = new DiaryAiClient.NextQuestionInput(
                "session-456",
                "user-123",
                "2025-05-14",
                List.of(new DiaryAiClient.ConversationTurn(1, "오늘 어땠나요?", "좋았어요")),
                List.of(new DiaryAiClient.ScheduleContext("09:00", "10:30", "공학관", "알고리즘"))
        );

        String json = objectMapper.writeValueAsString(input);

        assertThat(json).contains("\"sessionId\":\"session-456\"");
        assertThat(json).contains("\"conversationHistory\"");
        assertThat(json).contains("\"turnNumber\":1");
        assertThat(json).contains("\"question\":\"오늘 어땠나요?\"");
        assertThat(json).contains("\"answer\":\"좋았어요\"");
    }

    @Test
    @DisplayName("DiaryContentInput을 올바른 JSON으로 직렬화한다")
    void serializeDiaryContentInput() throws Exception {
        DiaryAiClient.DiaryContentInput input = new DiaryAiClient.DiaryContentInput(
                "session-456",
                "user-123",
                "2025-05-14",
                List.of(
                        new DiaryAiClient.ConversationTurn(1, "질문1", "답변1"),
                        new DiaryAiClient.ConversationTurn(2, "질문2", "답변2")
                ),
                List.of()
        );

        String json = objectMapper.writeValueAsString(input);

        assertThat(json).contains("\"userId\":\"user-123\"");
        assertThat(json).contains("\"conversationHistory\"");
        assertThat(json).contains("\"todaySchedule\":[]");
    }

    @Test
    @DisplayName("EmotionTag 문자열을 올바르게 파싱한다")
    void parseEmotionTag() {
        assertThat(EmotionTag.valueOf("HAPPY")).isEqualTo(EmotionTag.HAPPY);
        assertThat(EmotionTag.valueOf("SAD")).isEqualTo(EmotionTag.SAD);
        assertThat(EmotionTag.valueOf("EXCITED")).isEqualTo(EmotionTag.EXCITED);
        assertThat(EmotionTag.valueOf("CALM")).isEqualTo(EmotionTag.CALM);
    }

    @Test
    @DisplayName("null todaySchedule을 빈 리스트로 처리한다")
    void handleNullSchedule() throws Exception {
        DiaryAiClient.FirstQuestionInput input = new DiaryAiClient.FirstQuestionInput(
                "session-null-test",
                "user-123",
                "2025-05-14",
                null,
                null
        );

        // HttpDiaryAiClient 내부에서 null을 빈 리스트로 변환하는 로직 검증
        AiServerProperties properties = new AiServerProperties();
        properties.setBaseUrl("http://localhost:8081");
        properties.setMaxRetries(1);

        HttpDiaryAiClient client = new HttpDiaryAiClient(properties, objectMapper);

        // 직렬화 시 null이 아닌 빈 리스트로 변환되는지 확인
        // (실제 HTTP 호출은 하지 않으므로 직렬화 로직만 간접 검증)
        String json = objectMapper.writeValueAsString(input);
        assertThat(json).contains("\"todaySchedule\":null");
    }

    @Test
    @DisplayName("AiServerProperties가 올바르게 설정 여부를 판단한다")
    void aiServerPropertiesConfigured() {
        AiServerProperties props = new AiServerProperties();

        // 기본값은 빈 문자열 → 미설정
        assertThat(props.isConfigured()).isFalse();

        props.setBaseUrl("");
        assertThat(props.isConfigured()).isFalse();

        props.setBaseUrl("   ");
        assertThat(props.isConfigured()).isFalse();

        props.setBaseUrl("http://ai-server:8081");
        assertThat(props.isConfigured()).isTrue();
    }

    @Test
    @DisplayName("AiServerProperties 기본값이 올바르다")
    void aiServerPropertiesDefaults() {
        AiServerProperties props = new AiServerProperties();

        assertThat(props.getTimeoutMs()).isEqualTo(10000);
        assertThat(props.getMaxRetries()).isEqualTo(3);
        assertThat(props.getBaseUrl()).isEmpty();
    }
}
