package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpCookie;
import java.util.Map;
import java.util.regex.Pattern;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class PasswordResetIntegrationTest extends ContainerSupport {

    private static final Pattern RESET_LINK =
            Pattern.compile("http://localhost:3000/reset-password\\?token=([A-Za-z0-9_-]{43})");
    private static final String OLD_PASSWORD = "matkhau-manh-2026";
    private static final String NEW_PASSWORD = "matkhau-moi-2026";

    @Autowired private TestRestTemplate restTemplate;

    @Test
    @DisplayName(
            "Đặt lại mật khẩu bằng link trong mail: phiên cũ bị đăng xuất, chỉ mật khẩu mới dùng"
                    + " được")
    void resetPassword_endToEnd() throws Exception {
        String email = "quen.mat.khau@gmail.com";
        String oldRefreshToken = signUpAndSignIn("quen.mat.khau", email);

        ResponseEntity<Void> requested =
                restTemplate.postForEntity(
                        "/api/auth/password-reset", Map.of("email", email), Void.class);
        String token = readResetToken(email);
        ResponseEntity<Map> confirmed = confirm(token);
        ResponseEntity<Map> reused = confirm(token);

        assertThat(requested.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(confirmed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(reused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(refresh(oldRefreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login(email, OLD_PASSWORD).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(login(email, NEW_PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Email không có tài khoản vẫn nhận 202, không lộ thông tin")
    void requestReset_unknownEmail_returnsAccepted() {
        ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        "/api/auth/password-reset",
                        Map.of("email", "khong.co.ai@gmail.com"),
                        Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    private String signUpAndSignIn(String username, String email) throws Exception {
        restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username", username,
                        "email", email,
                        "password", OLD_PASSWORD,
                        "acceptedTerms", true),
                Map.class);
        String code =
                MailpitContainer.firstGroup(
                        MailpitContainer.CODE, MailpitContainer.client().awaitMailTo(email).text());
        ResponseEntity<Map> confirmed =
                restTemplate.postForEntity(
                        "/api/auth/email-verification/confirm",
                        Map.of("email", email, "code", code),
                        Map.class);
        return HttpCookie.parse(confirmed.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .get(0)
                .getValue();
    }

    private String readResetToken(String email) throws Exception {
        String text =
                MailpitContainer.client()
                        .awaitMailWithSubject(email, "Đặt lại mật khẩu SQL Judge")
                        .text();
        return MailpitContainer.firstGroup(RESET_LINK, text);
    }

    private ResponseEntity<Map> confirm(String token) {
        return restTemplate.postForEntity(
                "/api/auth/password-reset/confirm",
                Map.of("token", token, "newPassword", NEW_PASSWORD),
                Map.class);
    }

    private ResponseEntity<Map> login(String email, String password) {
        return restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("email", email, "password", password, "rememberMe", false),
                Map.class);
    }

    private ResponseEntity<Map> refresh(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ORIGIN, "http://localhost:3000");
        headers.add(HttpHeaders.COOKIE, "sqljudge_refresh=" + refreshToken);
        return restTemplate.exchange(
                "/api/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    }
}
