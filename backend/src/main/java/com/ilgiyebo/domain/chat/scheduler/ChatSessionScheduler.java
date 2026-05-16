package com.ilgiyebo.domain.chat.scheduler;

import com.ilgiyebo.domain.chat.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatSessionScheduler {

    private final ChatSessionService chatSessionService;

    @Scheduled(fixedRate = 30000)
    public void terminateExpiredSessions() {
        log.debug("Running expired session termination check");
        chatSessionService.endExpiredSessions();
    }
}
