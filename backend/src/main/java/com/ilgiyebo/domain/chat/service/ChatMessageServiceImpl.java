package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.entity.ChatMessageEntity;
import com.ilgiyebo.domain.chat.entity.ChatSessionEntity;
import com.ilgiyebo.domain.chat.exception.ChatException;
import com.ilgiyebo.domain.chat.repository.ChatMessageRepository;
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

        // Persist message
        ChatMessageEntity message = ChatMessageEntity.builder()
                .sessionId(sessionId)
                .senderId(senderId)
                .content(content)
                .build();

        return chatMessageRepository.save(message);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessageEntity> getMessagesBySessionId(UUID sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}
