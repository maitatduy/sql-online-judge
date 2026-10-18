package com.sqljudge.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

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

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "sqljudge.rate-limit.trusted-proxies=1")
class GatewayRateLimitBehindProxyTest extends GatewayIntegrationSupport {

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
            "Sau một proxy tin cậy: mỗi người dùng đếm theo IP proxy ghi lại, không dùng chung IP"
                    + " của proxy")
    void login_behindProxy_countsEachClientSeparately() {
        for (int attempt = 0; attempt < 10; attempt++) {
            login("203.0.113.1").expectStatus().isOk();
        }

        login("203.0.113.2").expectStatus().isOk();
        login("203.0.113.1").expectStatus().isEqualTo(429);
        List<String> keys =
                redisTemplate.keys("sqljudge:gateway:ratelimit:login:*").collectList().block();
        assertThat(keys)
                .containsExactlyInAnyOrder(
                        "sqljudge:gateway:ratelimit:login:203.0.113.1",
                        "sqljudge:gateway:ratelimit:login:203.0.113.2");
    }

    @Test
    @DisplayName(
            "Client tự chèn X-Forwarded-For giả phía trước: chỉ tin địa chỉ do proxy ghi cuối"
                    + " cùng, nên vẫn bị giới hạn")
    void login_spoofedLeadingForwardedFor_isIgnored() {
        for (int attempt = 0; attempt < 10; attempt++) {
            login("198.51.100." + attempt + ", 203.0.113.9").expectStatus().isOk();
        }

        login("198.51.100.99, 203.0.113.9").expectStatus().isEqualTo(429);
    }

    private WebTestClient.ResponseSpec login(String forwardedFor) {
        return webTestClient
                .post()
                .uri("/api/auth/login")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header("X-Forwarded-For", forwardedFor)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange();
    }
}
