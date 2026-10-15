package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthServiceApplicationTest {

    @Container @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4");

    @Autowired private TestRestTemplate restTemplate;

    @LocalManagementPort private int managementPort;

    @Test
    @DisplayName("Service khởi động với MySQL thật, Flyway chạy xong và health trả UP")
    void health_withRealMysql_isUp() {
        ResponseEntity<String> response =
                restTemplate.getForEntity(managementUrl("/actuator/health"), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Metric Prometheus chỉ có ở cổng quản trị, không có ở cổng API")
    void prometheus_isServedOnlyOnManagementPort() {
        ResponseEntity<String> managementResponse =
                restTemplate.getForEntity(managementUrl("/actuator/prometheus"), String.class);
        ResponseEntity<String> apiResponse =
                restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(managementResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(apiResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Endpoint cần đăng nhập trả 401 dạng ApiError")
    void protectedEndpoint_anonymous_returnsUnauthorized() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/users/me", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("\"code\":\"UNAUTHORIZED\"");
    }

    private String managementUrl(String path) {
        return "http://localhost:" + managementPort + path;
    }
}
