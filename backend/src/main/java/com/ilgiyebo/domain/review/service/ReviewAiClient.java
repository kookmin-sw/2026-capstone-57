package com.ilgiyebo.domain.review.service;

import java.util.List;

/**
 * 회고 AI 클라이언트 인터페이스.
 * 외부 AI 서버와 통신하여 회고 질문 생성 및 회고 글 생성을 담당한다.
 * diary 도메인의 DiaryAiClient와 동일한 패턴.
 */
public interface ReviewAiClient {

    /** 만남 컨텍스트 기반 회고 질문 생성 */
    List<String> generateReviewQuestions(String missionDescription, String missionLocation);

    /** 답변 기반 회고 글 자동 생성 */
    String generateReviewContent(List<QuestionAnswer> answers);

    record QuestionAnswer(String question, String answer) {}
}
