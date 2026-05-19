package com.ilgiyebo.domain.review.dto;

import com.ilgiyebo.domain.review.service.ReviewAiClient.ConversationTurn;

import java.util.List;
import java.util.UUID;

public record NextQuestionResponse(
    UUID sessionId,
    String question,
    boolean isConversationComplete,
    int currentTurn,
    int maxTurns,
    List<ConversationTurn> conversationHistory
) {}
