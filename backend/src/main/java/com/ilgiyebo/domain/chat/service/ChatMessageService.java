package com.ilgiyebo.domain.chat.service;

import com.ilgiyebo.domain.chat.entity.ChatMessageEntity;

import java.util.List;
import java.util.UUID;

public interface ChatMessageService {

    ChatMessageEntity saveMessage(UUID sessionId, UUID senderId, String content);

    List<ChatMessageEntity> getMessagesBySessionId(UUID sessionId);
}
