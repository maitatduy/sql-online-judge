package com.sqljudge.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRateLimitTest extends GatewayIntegrationSupport {

    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final AuthServiceStub AUTH_SERVICE = AuthServiceStub.start("{\"keys\":[]}", 200);

    @Autowired private ReactiveStringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", AUTH_SERVICE::baseUrl);
        registry.add("CORS_ALLOWED_ORIGINS", () -> FRONTEND_ORIGIN);
    }

    @AfterAll
    static void stopAuthServiceStub() {
        AUTH_SERVICE.stop();
    }

    @BeforeEach
    void resetCounters() {
        AUTH_SERVICE.clear();
        redisTemplate.execute(connection -> connection.serverCommands().flushAll()).blockLast();
    }

    @Test
    @DisplayName(
            "Đăng nhập: 10 lần/phút/IP qua được, lần 11 bị 429 ApiError kèm Retry-After và CORS")
    void login_overLimit_isRejectedWithRetryAfter() {
        for (int attempt = 0; attempt < 10; attempt++) {
            post("/api/auth/login").expectStatus().isOk();
        }

        post("/api/auth/login")
                .expectStatus()
                .isEqualTo(429)
                .expectHeader()
                .value(
                        HttpHeaders.RETRY_AFTER,
                        seconds -> assertThat(Long.parseLong(seconds)).isBetween(1L, 60L))
                .expectHeader()
                .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN)
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(429)
                .jsonPath("$.code")
                .isEqualTo("RATE_LIMITED")
                .jsonPath("$.message")
                .isEqualTo("Bạn thao tác quá nhanh, vui lòng thử lại sau");

        assertThat(AUTH_SERVICE.forwardedRequests()).hasSize(10);
    }

    @Test
    @DisplayName("Đăng ký chặt hơn: 5 lần/phút/IP")
    void register_overLimit_isRejected() {
        for (int attempt = 0; attempt < 5; attempt++) {
            post("/api/auth/register").expectStatus().isOk();
        }

        post("/api/auth/register").expectStatus().isEqualTo(429);
    }

    @Test
    @DisplayName("Mỗi chính sách đếm riêng; endpoint không có chính sách thì không bị giới hạn")
    void policies_areIndependentAndUnlistedPathsAreUnlimited() {
        for (int attempt = 0; attempt < 5; attempt++) {
            post("/api/auth/register").expectStatus().isOk();
        }

        post("/api/auth/login").expectStatus().isOk();
        post("/api/auth/email-verification").expectStatus().isOk();
        for (int attempt = 0; attempt < 15; attempt++) {
            webTestClient.get().uri("/api/auth/me").exchange().expectStatus().isOk();
        }
    }

    @Test
    @DisplayName("Bộ đếm nằm ở khoá sqljudge:gateway:ratelimit:<chính sách>:<ip> và có TTL")
    void counter_usesNamingConventionWithTtl() {
        post("/api/auth/login").expectStatus().isOk();

        List<String> keys =
                redisTemplate.keys("sqljudge:gateway:ratelimit:login:*").collectList().block();
        assertThat(keys).hasSize(1);
        String key = keys.get(0);
        assertThat(key).matches("sqljudge:gateway:ratelimit:login:[0-9a-f.:]+");
        assertThat(redisTemplate.opsForValue().get(key).block()).isEqualTo("1");
        Duration ttl = redisTemplate.getExpire(key).block();
        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(Duration.ofMinutes(1));
    }

    private WebTestClient.ResponseSpec post(String path) {
        return webTestClient
                .post()
                .uri(path)
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange();
    }
}
