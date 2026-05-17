package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.dto.QuizGenerateResponseMessage;
import com.ilgiyebo.domain.interaction.dto.QuizGenerateResponseMessage.AiQuizQuestion;
import com.ilgiyebo.domain.interaction.entity.QuizEntity;
import com.ilgiyebo.domain.interaction.repository.QuizRepository;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@org.springframework.context.annotation.Profile("!local")
@RequiredArgsConstructor
public class QuizResponseListener {

    private final UserRepository userRepository;
    private final QuizRepository quizRepository;
    private final ObjectMapper objectMapper;

    @SqsListener("${cloud.aws.sqs.quiz-response-queue}")
    @Transactional
    public void handleQuizResponse(String messageJson) {
        try {
            QuizGenerateResponseMessage response = objectMapper.readValue(
                    messageJson, QuizGenerateResponseMessage.class);

            if (!"SUCCESS".equals(response.status())) {
                log.warn("AI 퀴즈 생성 실패: userId={}, 상태={}", response.userId(), response.status());
                return;
            }

            if (response.quiz() == null || response.quiz().questions() == null
                    || response.quiz().questions().isEmpty()) {
                log.warn("AI 퀴즈 응답에 문항 데이터가 없습니다: userId={}", response.userId());
                return;
            }

            UUID userId = UUID.fromString(response.userId());
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("퀴즈 저장 대상 유저를 찾을 수 없습니다: userId={}", userId);
                return;
            }

            // 이미 퀴즈가 존재하면 중복 저장 방지
            if (quizRepository.existsByUserId(userId)) {
                log.info("이미 퀴즈가 존재합니다. 중복 저장 건너뜀: userId={}", userId);
                return;
            }

            List<AiQuizQuestion> aiQuestions = response.quiz().questions();

            for (int i = 0; i < aiQuestions.size(); i++) {
                AiQuizQuestion aiQ = aiQuestions.get(i);
                QuizEntity quiz = QuizEntity.builder()
                        .user(user)
                        .quizIndex(i + 1)
                        .question(aiQ.questionText())
                        .options(aiQ.choices())
                        .correctAnswer(aiQ.correctIndex())
                        .build();
                quizRepository.save(quiz);
            }

            log.info("퀴즈 데이터 저장 완료: userId={}, 문항수={}", userId, aiQuestions.size());

        } catch (Exception e) {
            log.error("AI 퀴즈 응답 처리 중 서버 에러 발생: {}", messageJson, e);
        }
    }
}
