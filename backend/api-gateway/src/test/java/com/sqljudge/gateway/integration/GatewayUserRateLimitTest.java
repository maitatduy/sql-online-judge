package com.sqljudge.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.RSAKey;
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
class GatewayUserRateLimitTest extends GatewayIntegrationSupport {

    private static final RSAKey SIGNING_KEY = TestJwts.newKey();
    private static final AuthServiceStub AUTH_SERVICE =
            AuthServiceStub.start(TestJwts.jwksOf(SIGNING_KEY), 200);
    private static final String PROBLEM_PATH = "/api/problems/0192f3d0-0000-7000-8000-000000000005";
    private static final String OTHER_USER = "0192f3a0-0000-7000-8000-000000000002";

    @Autowired private ReactiveStringRedisTemplate redisTemplate;

    @DynamicPropertySource
    static void gatewayProperties(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", AUTH_SERVICE::baseUrl);
        registry.add("CORS_ALLOWED_ORIGINS", () -> "http://localhost:3000");
    }

    @AfterAll
    static void stopAuthServiceStub() {
        AUTH_SERVICE.stop();
    }

    @BeforeEach
    void resetCounters() {
        redisTemplate.execute(connection -> connection.serverCommands().flushAll()).blockLast();
    }

    @Test
    @DisplayName("Nộp bài: 6 lần/phút/người, lần 7 bị 429; người khác vẫn nộp được")
    void submit_overLimitPerUser_isRejectedOnlyForThatUser() {
        String learner = TestJwts.validToken(SIGNING_KEY, "LEARNER");
        for (int attempt = 0; attempt < 6; attempt++) {
            post("/submissions", learner).expectStatus().isNotFound();
        }

        post("/submissions", learner)
                .expectStatus()
                .isEqualTo(429)
                .expectHeader()
                .exists(HttpHeaders.RETRY_AFTER)
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("RATE_LIMITED");
        post("/submissions", TestJwts.learnerToken(SIGNING_KEY, OTHER_USER))
                .expectStatus()
                .isNotFound();
        assertThat(SUBMISSION_SERVICE.forwardedRequests()).hasSize(7);
    }

    @Test
    @DisplayName(
            "Chạy thử: 20 lần/phút/người, đếm riêng với nộp bài; đọc lịch sử không bị giới hạn")
    void run_hasOwnLimitAndReadsAreUnlimited() {
        String learner = TestJwts.validToken(SIGNING_KEY, "LEARNER");
        for (int attempt = 0; attempt < 20; attempt++) {
            post("/runs", learner).expectStatus().isNotFound();
        }

        post("/runs", learner).expectStatus().isEqualTo(429);
        post("/submissions", learner).expectStatus().isNotFound();
        for (int attempt = 0; attempt < 10; attempt++) {
            webTestClient
                    .get()
                    .uri(PROBLEM_PATH + "/submissions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + learner)
                    .exchange()
                    .expectStatus()
                    .isNotFound();
        }
    }

    @Test
    @DisplayName("Bộ đếm theo người nằm ở khoá sqljudge:gateway:ratelimit:submit:<userId>")
    void counter_isKeyedByUserId() {
        post("/submissions", TestJwts.validToken(SIGNING_KEY, "LEARNER"))
                .expectStatus()
                .isNotFound();

        List<String> keys =
                redisTemplate.keys("sqljudge:gateway:ratelimit:submit:*").collectList().block();
        assertThat(keys).containsExactly("sqljudge:gateway:ratelimit:submit:" + TestJwts.USER_ID);
    }

    private WebTestClient.ResponseSpec post(String suffix, String bearer) {
        return webTestClient
                .post()
                .uri(PROBLEM_PATH + suffix)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearer)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange();
    }
}
