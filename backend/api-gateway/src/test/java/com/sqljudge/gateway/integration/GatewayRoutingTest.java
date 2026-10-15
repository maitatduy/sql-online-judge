package com.sqljudge.gateway.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class GatewayRoutingTest {

    private static final String FRONTEND_ORIGIN = "http://localhost:3000";
    private static final AuthServiceStub AUTH_SERVICE = AuthServiceStub.start("{\"keys\":[]}", 401);

    @Autowired private WebTestClient webTestClient;

    @LocalManagementPort private int managementPort;

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
    @DisplayName("Request /api/auth/** được chuyển tới auth-service, giữ nguyên status và ApiError")
    void authPath_isRoutedToAuthService() {
        webTestClient
                .get()
                .uri("/api/auth/me")
                .exchange()
                .expectStatus()
                .isUnauthorized()
                .expectBody()
                .jsonPath("$.code")
                .isEqualTo("UNAUTHORIZED");

        assertThat(AUTH_SERVICE.forwardedRequestLines()).containsExactly("GET /api/auth/me");
    }

    @Test
    @DisplayName("Request /api/users/** được chuyển tới auth-service kèm query string")
    void usersPath_isRoutedToAuthServiceWithQuery() {
        webTestClient
                .get()
                .uri("/api/users/availability?username=minh.anh")
                .exchange()
                .expectStatus()
                .isUnauthorized();

        assertThat(AUTH_SERVICE.forwardedRequestLines())
                .containsExactly("GET /api/users/availability?username=minh.anh");
    }

    @Test
    @DisplayName("Đường dẫn nội bộ /internal/** không được định tuyến ra ngoài")
    void internalPath_isNotRouted() {
        webTestClient.get().uri("/internal/jwks").exchange().expectStatus().isNotFound();

        assertThat(AUTH_SERVICE.forwardedRequestLines()).isEmpty();
    }

    @Test
    @DisplayName("Preflight từ origin của frontend được chấp nhận, có credentials")
    void preflight_fromFrontendOrigin_isAllowed() {
        webTestClient
                .options()
                .uri("/api/auth/login")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                .header(
                        HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                        HttpHeaders.AUTHORIZATION + "," + HttpHeaders.CONTENT_TYPE)
                .exchange()
                .expectStatus()
                .isOk()
                .expectHeader()
                .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN)
                .expectHeader()
                .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");

        assertThat(AUTH_SERVICE.forwardedRequestLines()).isEmpty();
    }

    @Test
    @DisplayName("Preflight từ origin lạ bị từ chối")
    void preflight_fromUnknownOrigin_isRejected() {
        webTestClient
                .options()
                .uri("/api/auth/login")
                .header(HttpHeaders.ORIGIN, "https://evil.example")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, HttpMethod.POST.name())
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectHeader()
                .doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    @Test
    @DisplayName("Response thật tới frontend có header CORS và lộ Retry-After")
    void routedResponse_toFrontendOrigin_hasCorsHeaders() {
        webTestClient
                .get()
                .uri("/api/auth/me")
                .header(HttpHeaders.ORIGIN, FRONTEND_ORIGIN)
                .exchange()
                .expectHeader()
                .valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND_ORIGIN)
                .expectHeader()
                .value(
                        HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS,
                        exposed -> assertThat(exposed).contains(HttpHeaders.RETRY_AFTER));
    }

    @Test
    @DisplayName("Metric Prometheus chỉ có ở cổng quản trị, không có ở cổng API")
    void prometheus_isServedOnlyOnManagementPort() {
        WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + managementPort)
                .build()
                .get()
                .uri("/actuator/prometheus")
                .exchange()
                .expectStatus()
                .isOk();

        webTestClient.get().uri("/actuator/prometheus").exchange().expectStatus().isNotFound();
    }
}
