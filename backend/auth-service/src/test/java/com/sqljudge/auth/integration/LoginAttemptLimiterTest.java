package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.service.LoginAttemptLimiter;
import com.sqljudge.auth.service.SecureTokens;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

class LoginAttemptLimiterTest extends ContainerSupport {

    private static final int REDIS_PORT = 6379;
    private static final LettuceConnectionFactory CONNECTION_FACTORY = connectToRedisContainer();
    private static final StringRedisTemplate REDIS_TEMPLATE =
            new StringRedisTemplate(CONNECTION_FACTORY);

    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter(REDIS_TEMPLATE);

    @AfterAll
    static void closeConnection() {
        CONNECTION_FACTORY.destroy();
    }

    @BeforeEach
    void flushRedis() {
        CONNECTION_FACTORY.getConnection().serverCommands().flushAll();
    }

    @Test
    @DisplayName("Sai 9 lần chưa bị khoá, lần thứ 10 bị khoá khoảng 15 phút")
    void tenthFailure_locksForFifteenMinutes() {
        for (int failure = 0; failure < 9; failure++) {
            limiter.recordFailure("minh.anh@gmail.com");
        }
        assertThat(limiter.lockRemaining("minh.anh@gmail.com")).isEmpty();

        limiter.recordFailure("minh.anh@gmail.com");

        assertThat(limiter.lockRemaining("minh.anh@gmail.com"))
                .hasValueSatisfying(
                        remaining ->
                                assertThat(remaining)
                                        .isGreaterThan(Duration.ofMinutes(14))
                                        .isLessThanOrEqualTo(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Khoá Redis theo quy ước sqljudge:auth:*, dùng hash email và luôn có TTL")
    void keys_followNamingConventionAndExpire() {
        limiter.recordFailure("minh.anh@gmail.com");

        String failuresKey =
                "sqljudge:auth:login-failures:" + SecureTokens.sha256("minh.anh@gmail.com");
        assertThat(REDIS_TEMPLATE.opsForValue().get(failuresKey)).isEqualTo("1");
        assertThat(REDIS_TEMPLATE.getExpire(failuresKey)).isBetween(1L, 15L * 60);
        assertThat(REDIS_TEMPLATE.keys("*minh.anh*")).isEmpty();
    }

    @Test
    @DisplayName("Đăng nhập đúng thì xoá bộ đếm; email khác không bị ảnh hưởng")
    void reset_clearsFailuresOfThatEmailOnly() {
        for (int failure = 0; failure < 9; failure++) {
            limiter.recordFailure("minh.anh@gmail.com");
            limiter.recordFailure("thu.ha@gmail.com");
        }

        limiter.reset("minh.anh@gmail.com");
        limiter.recordFailure("minh.anh@gmail.com");
        limiter.recordFailure("thu.ha@gmail.com");

        assertThat(limiter.lockRemaining("minh.anh@gmail.com")).isEmpty();
        assertThat(limiter.lockRemaining("thu.ha@gmail.com")).isPresent();
    }

    private static LettuceConnectionFactory connectToRedisContainer() {
        LettuceConnectionFactory factory =
                new LettuceConnectionFactory(
                        new RedisStandaloneConfiguration(
                                REDIS.getHost(), REDIS.getMappedPort(REDIS_PORT)));
        factory.afterPropertiesSet();
        factory.start();
        return factory;
    }
}
