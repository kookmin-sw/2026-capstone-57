package com.ilgiyebo.domain.chat.entity;

import com.ilgiyebo.common.entity.BaseSchema;
import com.ilgiyebo.domain.matching.entity.MatchEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_session")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = true)
public class ChatSessionEntity extends BaseSchema {

    @Column(name = "match_id", nullable = false)
    private UUID matchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", insertable = false, updatable = false)
    private MatchEntity match;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ChatSessionStatus status = ChatSessionStatus.ACTIVE;

    @Builder.Default
    @Column(name = "token_limit", nullable = false)
    private int tokenLimit = 150;

    @Builder.Default
    @Column(name = "used_tokens", nullable = false)
    private int usedTokens = 0;

    @Column(name = "icebreaker_question")
    private String icebreakerQuestion;
}
