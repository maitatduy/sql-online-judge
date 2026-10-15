package com.sqljudge.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.gateway.integration.AuthServiceStub.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayAuthenticationTest {

    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final RSAKey SIGNING_KEY = TestJwts.newKey();
    private static final AuthServiceStub AUTH_SERVICE =
            AuthServiceStub.start(TestJwts.jwksOf(SIGNING_KEY), 200);

    @Autowired private WebTestClient webTestClient;

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
    void clearRecordedRequests() {
        AUTH_SERVICE.clear();
    }

    @Test
    @DisplayName("JWT hợp lệ: chuyển tiếp Authorization và X-User-* lấy từ token")
    void validJwt_forwardsAuthorizationAndUserHeaders() {
        String token = TestJwts.validToken(SIGNING_KEY, "LEARNER");

        webTestClient
                .get()
                .uri("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus()
                .isOk();

        RecordedRequest forwarded = onlyForwardedRequest();
        assertThat(forwarded.header(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer " + token);
        assertThat(forwarded.header("X-User-Id")).containsExactly(TestJwts.USER_ID);
        assertThat(forwarded.header("X-User-Role")).containsExactly("LEARNER");
    }

    @Test
    @DisplayName("Client tự gửi X-User-Role: ADMIN kèm JWT LEARNER thì service chỉ thấy LEARNER")
    void validJwt_clientUserHeaders_areReplacedByTokenClaims() {
        webTestClient
                .get()
                .uri("/api/auth/me")
                .header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + TestJwts.validToken(SIGNING_KEY, "LEARNER"))
                .header("X-User-Role", "ADMIN")
                .header("X-User-Id", "0192f3a0-0000-7000-8000-0000000000ad")
                .header("X-User-Anything", "spoofed")
                .exchange()
                .expectStatus()
                .isOk();

        RecordedRequest forwarded = onlyForwardedRequest();
        assertThat(forwarded.header("X-User-Role")).containsExactly("LEARNER");
        assertThat(forwarded.header("X-User-Id")).containsExactly(TestJwts.USER_ID);
        assertThat(forwarded.header("X-User-Anything")).isEmpty();
    }

    @Test
    @DisplayName("Request ẩn danh vẫn đi qua nhưng mọi header X-User-* của client bị xoá")
    void anonymous_userHeadersAreStripped() {
        webTestClient
                .get()
                .uri("/api/users/availability?username=minh.anh")
                .header("X-User-Role", "ADMIN")
                .header("X-User-Id", TestJwts.USER_ID)
                .exchange()
                .expectStatus()
                .isOk();

        RecordedRequest forwarded = onlyForwardedRequest();
        assertThat(forwarded.header("X-User-Role")).isEmpty();
        assertThat(forwarded.header("X-User-Id")).isEmpty();
    }

    @Test
    @DisplayName("JWT ký bằng khoá khác: gateway trả 401 ApiError, không gọi service")
    void forgedJwt_isRejectedAtGateway() {
        String forged = TestJwts.validToken(TestJwts.newKey(), "ADMIN");

        webTestClient
                .get()
                .uri("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + forged)
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.status")
                .isEqualTo(401)
                .jsonPath("$.code")
                .isEqualTo("UNAUTHORIZED")
                .jsonPath("$.message")
                .isEqualTo("Bạn cần đăng nhập để tiếp tục")
                .jsonPath("$.timestamp")
                .exists();

        assertThat(AUTH_SERVICE.forwardedRequests()).isEmpty();
    }

    @Test
    @DisplayName("JWT hết hạn: gateway trả 401, không gọi service")
    void expiredJwt_isRejectedAtGateway() {
        webTestClient
                .get()
                .uri("/api/auth/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.expiredToken(SIGNING_KEY))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("UNAUTHORIZED");

        assertThat(AUTH_SERVICE.forwardedRequests()).isEmpty();
    }

    @Test
    @DisplayName("401 của gateway có header CORS để frontend đọc được và tự refresh")
    void rejectedJwt_fromFrontendOrigin_hasCorsHeaders() {
        webTestClient
                .get()
                .uri("/api/auth/me")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + TestJwts.expiredToken(SIGNING_KEY))
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectHeader()
                .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN)
                .expectHeader()
                .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    private static RecordedRequest onlyForwardedRequest() {
        assertThat(AUTH_SERVICE.forwardedRequests()).hasSize(1);
        return AUTH_SERVICE.forwardedRequests().get(0);
    }
}
