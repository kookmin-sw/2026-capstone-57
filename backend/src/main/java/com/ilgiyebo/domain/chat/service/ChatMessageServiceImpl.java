package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.entity.ChatMessageEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionStatus;
import com.ilgiyebo.domain.chat.exception.ChatException;
import com.ilgiyebo.domain.chat.repository.ChatMessageRepository;
import com.ilgiyebo.domain.chat.repository.ChatSessionRepository;
import com.ilgiyebo.domain.interaction.service.InteractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatSessionService chatSessionService;
    private final ChatSessionRepository chatSessionRepository;
    private final InteractionService interactionService;

    @Override
    @Transactional
    public ChatMessageEntity saveMessage(UUID sessionId, UUID senderId, String content) {
        // Validate content is not blank
        if (content == null || content.isBlank()) {
            throw ChatException.INVALID_MESSAGE.toException();
        }

        // Validate session is ACTIVE (check Redis cache first, fallback to DB)
        chatSessionService.validateSessionActive(sessionId);

        // Validate sender is a participant of the session's match
        ChatSessionEntity session = chatSessionService.getSessionById(sessionId);
        chatSessionService.validateParticipant(senderId, session.getMatchId());

        // Check token limit before saving
        int messageTokens = content.length();
        if (session.getUsedTokens() + messageTokens > session.getTokenLimit()) {
            // Token limit reached — end session and advance stage
            chatSessionService.endSession(sessionId);
            interactionService.completeChat(session.getMatchId());
            throw ChatException.TOKEN_LIMIT_REACHED.toException();
        }

        // Accumulate tokens
        session.setUsedTokens(session.getUsedTokens() + messageTokens);
        chatSessionRepository.save(session);

        // Persist message
        ChatMessageEntity message = ChatMessageEntity.builder()
                .sessionId(sessionId)
                .senderId(senderId)
                .content(content)
                .build();

        ChatMessageEntity saved = chatMessageRepository.save(message);

        // Check if token limit reached after saving this message
        if (session.getUsedTokens() >= session.getTokenLimit()) {
            chatSessionService.endSession(sessionId);
            interactionService.completeChat(session.getMatchId());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageEntity> getMessagesBySessionId(UUID sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}
