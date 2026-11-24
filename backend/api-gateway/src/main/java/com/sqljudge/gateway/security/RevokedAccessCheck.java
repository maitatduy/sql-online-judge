package com.sqljudge.gateway.security;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RevokedAccessCheck {

    private static final Logger log = LoggerFactory.getLogger(RevokedAccessCheck.class);

    static final String SESSION_KEY_PREFIX = "sqljudge:auth:revoked-session:";
    static final String USER_KEY_PREFIX = "sqljudge:auth:revoked-user:";
    private static final String SESSION_CLAIM = "sid";
    private static final String NO_SESSION = "none";

    private final ReactiveStringRedisTemplate redisTemplate;

    public RevokedAccessCheck(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Mono<Jwt> verify(Jwt jwt) {
        String sessionId = jwt.getClaimAsString(SESSION_CLAIM);
        List<String> keys =
                List.of(
                        SESSION_KEY_PREFIX + (sessionId == null ? NO_SESSION : sessionId),
                        USER_KEY_PREFIX + jwt.getSubject());
        return redisTemplate
                .opsForValue()
                .multiGet(keys)
                .flatMap(values -> isRevoked(jwt, values) ? revoked() : Mono.just(jwt))
                .onErrorResume(
                        error -> !(error instanceof BadJwtException),
                        error -> {
                            log.warn("revocation_check_unavailable", error);
                            return Mono.just(jwt);
                        });
    }

    private static boolean isRevoked(Jwt jwt, List<String> values) {
        if (values.get(0) != null) {
            return true;
        }
        String revokedAt = values.get(1);
        Instant issuedAt = jwt.getIssuedAt();
        return revokedAt != null
                && issuedAt != null
                && issuedAt.getEpochSecond() < Long.parseLong(revokedAt);
    }

    private static Mono<Jwt> revoked() {
        return Mono.error(new BadJwtException("Phiên đăng nhập đã bị thu hồi"));
    }
}
