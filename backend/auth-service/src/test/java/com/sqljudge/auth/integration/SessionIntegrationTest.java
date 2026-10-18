package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpCookie;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SessionIntegrationTest extends ContainerSupport {

    private static final String FRONTEND_ORIGIN = "http://localhost:3000";

    @Autowired private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        MailpitContainer.registerMailProperties(registry);
    }

    @Test
    @DisplayName(
            "Vòng đời phiên: refresh xoay vòng, token cũ dùng ngay thì vẫn được, đăng xuất thì hết")
    void sessionLifecycle_refreshRotatesAndLogoutRevokes() throws Exception {
        String firstToken = signUpAndSignIn("vong.doi", "vong.doi@gmail.com");

        ResponseEntity<Map> refreshed = post("/api/auth/refresh", firstToken, FRONTEND_ORIGIN);
        String secondToken = refreshCookieValue(refreshed);
        ResponseEntity<Map> concurrentTab = post("/api/auth/refresh", firstToken, FRONTEND_ORIGIN);
        ResponseEntity<Map> logout = post("/api/auth/logout", secondToken, FRONTEND_ORIGIN);
        ResponseEntity<Map> afterLogout = post("/api/auth/refresh", secondToken, FRONTEND_ORIGIN);

        assertThat(refreshed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(secondToken).isNotEqualTo(firstToken);
        assertThat(me((String) refreshed.getBody().get("accessToken")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(concurrentTab.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(concurrentTab.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(afterLogout.getBody()).containsEntry("code", "INVALID_REFRESH_TOKEN");
    }

    @Test
    @DisplayName("Refresh không có Origin của frontend: 403, phiên không bị xoay")
    void refresh_withoutTrustedOrigin_isRejected() throws Exception {
        String token = signUpAndSignIn("khong.origin", "khong.origin@gmail.com");

        ResponseEntity<Map> rejected = post("/api/auth/refresh", token, "https://evil.example");
        ResponseEntity<Map> stillValid = post("/api/auth/refresh", token, FRONTEND_ORIGIN);

        assertThat(rejected.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(stillValid.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private String signUpAndSignIn(String username, String email) throws Exception {
        restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username",
                        username,
                        "email",
                        email,
                        "password",
                        "matkhau-manh-2026",
                        "acceptedTerms",
                        true),
                Map.class);
        String code =
                MailpitContainer.firstGroup(
                        MailpitContainer.CODE, MailpitContainer.client().awaitMailTo(email).text());
        ResponseEntity<Map> confirmed =
                restTemplate.postForEntity(
                        "/api/auth/email-verification/confirm",
                        Map.of("email", email, "code", code),
                        Map.class);
        return refreshCookieValue(confirmed);
    }

    private ResponseEntity<Map> post(String path, String refreshToken, String origin) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ORIGIN, origin);
        headers.add(HttpHeaders.COOKIE, "sqljudge_refresh=" + refreshToken);
        return restTemplate.exchange(path, HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    }

    private ResponseEntity<Map> me(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private static String refreshCookieValue(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).as("Set-Cookie of %s", response.getStatusCode()).isNotNull();
        return HttpCookie.parse(setCookie).get(0).getValue();
    }
}
