package com.sqljudge.auth.service;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginAttemptLimiter {

    static final int MAX_FAILURES = 10;
    static final Duration FAILURE_WINDOW = Duration.ofMinutes(15);
    static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private static final String FAILURES_KEY_PREFIX = "sqljudge:auth:login-failures:";
    private static final String LOCK_KEY_PREFIX = "sqljudge:auth:login-lock:";

    private final StringRedisTemplate redisTemplate;

    public Optional<Duration> lockRemaining(String email) {
        try {
            Long seconds = redisTemplate.getExpire(lockKey(email));
            if (seconds == null || seconds <= 0) {
                return Optional.empty();
            }
            return Optional.of(Duration.ofSeconds(seconds));
        } catch (DataAccessException exception) {
            log.warn("login_limiter_unavailable reason={}", exception.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public void recordFailure(String email) {
        try {
            String failuresKey = failuresKey(email);
            Long failures = redisTemplate.opsForValue().increment(failuresKey);
            if (failures != null && failures == 1) {
                redisTemplate.expire(failuresKey, FAILURE_WINDOW);
            }
            if (failures != null && failures >= MAX_FAILURES) {
                redisTemplate.opsForValue().set(lockKey(email), "1", LOCK_DURATION);
                redisTemplate.delete(failuresKey);
                log.warn("login_locked emailHash={}", SecureTokens.sha256(email));
            }
        } catch (DataAccessException exception) {
            log.warn("login_limiter_unavailable reason={}", exception.getClass().getSimpleName());
        }
    }

    public void reset(String email) {
        try {
            redisTemplate.delete(failuresKey(email));
        } catch (DataAccessException exception) {
            log.warn("login_limiter_unavailable reason={}", exception.getClass().getSimpleName());
        }
    }

    private static String failuresKey(String email) {
        return FAILURES_KEY_PREFIX + SecureTokens.sha256(email);
    }

    private static String lockKey(String email) {
        return LOCK_KEY_PREFIX + SecureTokens.sha256(email);
    }
}
