package com.sqljudge.auth.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccessRevocations {

    static final String SESSION_KEY_PREFIX = "sqljudge:auth:revoked-session:";
    static final String USER_KEY_PREFIX = "sqljudge:auth:revoked-user:";

    private final StringRedisTemplate redisTemplate;

    public void revokeSession(UUID sessionId) {
        redisTemplate
                .opsForValue()
                .set(SESSION_KEY_PREFIX + sessionId, "1", AccessTokenIssuer.ACCESS_TOKEN_TTL);
    }

    public void revokeUser(UUID userId, Instant revokedAt) {
        redisTemplate
                .opsForValue()
                .set(
                        USER_KEY_PREFIX + userId,
                        Long.toString(revokedAt.getEpochSecond()),
                        AccessTokenIssuer.ACCESS_TOKEN_TTL);
    }
}
