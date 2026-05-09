package com.ilgiyebo.domain.interaction.service;

import com.ilgiyebo.domain.interaction.entity.InteractionEntity;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import com.ilgiyebo.domain.user.entity.UserEntity;
import com.ilgiyebo.domain.interaction.dto.QuizGenerateRequestMessage.TargetProfile;
import com.ilgiyebo.domain.interaction.dto.QuizQuestionDto;
import com.ilgiyebo.domain.interaction.dto.QuizQuestionResponse;
import com.ilgiyebo.domain.interaction.dto.QuizResponseDto;
import com.ilgiyebo.domain.interaction.dto.QuizSubmitRequest;
import com.ilgiyebo.domain.interaction.entity.StageStatus;
import com.ilgiyebo.domain.interaction.exception.InteractionException;
import com.ilgiyebo.domain.interaction.repository.InteractionRepository;
import com.ilgiyebo.domain.matching.repository.MatchRepository;
import com.ilgiyebo.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private final InteractionRepository interactionRepository;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final InteractionService interactionService;
    private final QuizRequestPublisher quizRequestPublisher;

    @Override
    @Transactional
    public List<QuizQuestionResponse> getQuiz(UUID matchId, UUID userId) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);
        validateUserInMatch(match, userId);

        InteractionEntity interaction = interactionRepository.findByMatchId(matchId)
                .orElseThrow(InteractionException.INTERACTION_NOT_FOUND::toException);

        if (interaction.getCurrentStage() != 1) {
            throw InteractionException.NOT_IN_QUIZ_STAGE.toException();
        }

        boolean isUserA = match.getUserA().getId().equals(userId);

        // 1. DB에 해당 유저의 퀴즈가 이미 존재하면 정답 제외하고 반환
        List<QuizQuestionDto> quizData = isUserA
                ? interaction.getQuizDataA()
                : interaction.getQuizDataB();

        if (quizData != null && !quizData.isEmpty()) {
            return quizData.stream()
                    .map(QuizQuestionResponse::from)
                    .toList();
        }

        // 2. 이미 SQS 요청을 보낸 상태면 중복 발행 없이 대기 응답
        boolean quizRequested = isUserA
                ? interaction.isQuizRequestedA()
                : interaction.isQuizRequestedB();

        if (quizRequested) {
            throw InteractionException.QUIZ_GENERATING.toException();
        }

        // 3. DB에 퀴즈가 없고 요청도 안 보낸 상태 → SQS를 통해 AI 생성 요청
        UUID partnerId = isUserA
                ? match.getUserB().getId()
                : match.getUserA().getId();

        UserEntity partner = userRepository.findById(partnerId)
                .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);

        TargetProfile targetProfile = TargetProfile.builder()
                .name(partner.getName())
                .nickname(partner.getNickname())
                .university(partner.getUniversity())
                .major(partner.getMajor())
                .hobbies(partner.getHobbies())
                .interests(partner.getInterests())
                .personalityType(partner.getPersonalityTypes())
                .build();

        quizRequestPublisher.requestQuizGeneration(matchId, userId, partnerId, targetProfile);

        if (isUserA) {
            interaction.setQuizRequestedA(true);
        } else {
            interaction.setQuizRequestedB(true);
        }
        interactionRepository.save(interaction);
        log.info("AI 퀴즈 생성 요청 완료: 매칭ID={}, 요청유저ID={}, 대상유저ID={}", matchId, userId, partnerId);

        // 4. 퀴즈가 아직 준비되지 않음 (생성 중)
        throw InteractionException.QUIZ_GENERATING.toException();
    }

    @Override
    @Transactional
    public QuizResponseDto submitQuiz(UUID matchId, UUID userId, QuizSubmitRequest request) {
        MatchEntity match = matchRepository.findById(matchId)
                .orElseThrow(InteractionException.MATCH_NOT_FOUND::toException);
        validateUserInMatch(match, userId);

        InteractionEntity interaction = interactionRepository.findByMatchId(matchId)
                .orElseThrow(InteractionException.INTERACTION_NOT_FOUND::toException);

        if (interaction.getCurrentStage() != 1) {
            throw InteractionException.NOT_IN_QUIZ_STAGE.toException();
        }

        if (interaction.getStageStatus() == StageStatus.TERMINATED) {
            throw InteractionException.ALREADY_TERMINATED.toException();
        }

        boolean isUserA = match.getUserA().getId().equals(userId);

        List<QuizQuestionDto> questions = isUserA
                ? interaction.getQuizDataA()
                : interaction.getQuizDataB();

        if (questions == null || questions.isEmpty()) {
            throw InteractionException.QUIZ_NOT_GENERATED.toException();
        }

        int quizIndex = request.quizIndex();
        int userAnswer = request.answer();

        // 해당 인덱스의 퀴즈 찾기
        QuizQuestionDto targetQuestion = questions.stream()
                .filter(q -> q.quizIndex() == quizIndex)
                .findFirst()
                .orElseThrow(InteractionException.QUIZ_NOT_GENERATED::toException);

        // 해당 문항에 유저 답안 기록
        List<QuizQuestionDto> updatedQuestions = questions.stream()
                .map(q -> q.quizIndex() == quizIndex
                        ? new QuizQuestionDto(q.quizIndex(), q.question(), q.options(), q.correctAnswer(), userAnswer)
                        : q)
                .toList();

        if (isUserA) {
            interaction.setQuizDataA(updatedQuestions);
        } else {
            interaction.setQuizDataB(updatedQuestions);
        }

        // 전체 정답 수 계산
        int totalCount = updatedQuestions.size();
        int correctCount = (int) updatedQuestions.stream()
                .filter(q -> q.quizAnswer() != null && q.quizAnswer().equals(q.correctAnswer()))
                .count();

        // 모든 문항을 풀었는지 확인
        boolean allCompleted = updatedQuestions.stream()
                .allMatch(q -> q.quizAnswer() != null);

        interactionRepository.save(interaction);

        // 모든 문항 완료 시 퀴즈 단계 완료 처리
        if (allCompleted) {
            interactionService.completeQuiz(matchId, userId);
        }

        return QuizResponseDto.from(
                matchId,
                quizIndex,
                targetQuestion.correctAnswer(),
                userAnswer,
                correctCount,
                totalCount,
                allCompleted
        );
    }

    private void validateUserInMatch(MatchEntity match, UUID userId) {
        if (!match.getUserA().getId().equals(userId) && !match.getUserB().getId().equals(userId)) {
            throw InteractionException.USER_NOT_IN_MATCH.toException();
        }
    }
}
