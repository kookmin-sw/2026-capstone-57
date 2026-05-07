package com.ilgiyebo.domain.ai.quiz.service;

import com.ilgiyebo.domain.ai.quiz.dto.QuizRequestMessage;
import com.ilgiyebo.domain.ai.quiz.dto.QuizResponseMessage;

public interface AiQuizService {
    QuizResponseMessage generateQuiz(QuizRequestMessage request);
}
