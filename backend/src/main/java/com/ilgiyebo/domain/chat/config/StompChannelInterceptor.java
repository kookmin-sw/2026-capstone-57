package com.ilgiyebo.domain.chat.config;

import com.ilgiyebo.config.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StompChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = resolveToken(accessor);

            if (token == null || !jwtTokenProvider.validateToken(token)) {
                throw new IllegalArgumentException("유효하지 않은 토큰입니다");
            }

            UUID userId = jwtTokenProvider.getUserIdFromToken(token);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null,
                            List.of(new SimpleGrantedAuthority("ROLE_USER")));
            accessor.setUser(authentication);
        }

        return message;
    }

    private String resolveToken(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearer = authHeaders.get(0);
            if (bearer.startsWith("Bearer ")) {
                return bearer.substring(7);
            }
        }
        return null;
    }
}
