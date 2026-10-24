package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.util.Map;
import java.util.UUID;
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
class AdminUserEmailIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminActionRepository adminActionRepository;

    @Test
    @DisplayName(
            "Admin gửi link đặt lại mật khẩu: 202, người dùng nhận mail, gửi ngay lần nữa bị 429")
    void sendPasswordResetEmail_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.thu", "admin.thu@gmail.com");
        String email = "quen.matkhau@gmail.com";
        registerVerified("quen.matkhau", email);
        String path = "/api/admin/users/quen.matkhau/password-reset-email";

        ResponseEntity<Map> sent = post(path, adminToken);
        ResponseEntity<Map> again = post(path, adminToken);

        assertThat(sent.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(sent.getBody()).containsEntry("username", "quen.matkhau");
        assertThat(
                        MailpitContainer.client()
                                .awaitMailWithSubject(email, "Đặt lại mật khẩu SQL Judge")
                                .text())
                .contains("/reset-password");
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(again.getBody()).containsEntry("code", "EMAIL_RESEND_COOLDOWN");
        assertThat(Long.parseLong(again.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)))
                .isBetween(1L, 60L);
        assertSingleAction("quen.matkhau", AdminActionType.SEND_PASSWORD_RESET);
    }

    @Test
    @DisplayName(
            "Admin gửi lại mã xác minh cho tài khoản chưa xác minh: 202 và có mail; đã xác minh"
                    + " thì 409")
    void sendVerificationEmail_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.lan", "admin.lan@gmail.com");
        String email = "cho.xacminh@gmail.com";
        restTemplate.exchange(
                "/api/admin/users",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("username", "cho.xacminh", "email", email, "role", "LEARNER"),
                        bearer(adminToken)),
                Map.class);

        ResponseEntity<Map> sent =
                post("/api/admin/users/cho.xacminh/verification-email", adminToken);
        ResponseEntity<Map> alreadyVerified =
                post("/api/admin/users/admin.lan/verification-email", adminToken);
        ResponseEntity<Map> missing =
                post("/api/admin/users/khong.co.ai/verification-email", adminToken);

        assertThat(sent.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(
                        MailpitContainer.client()
                                .awaitMailWithSubject(email, "Xác thực email SQL Judge"))
                .isNotNull();
        assertThat(alreadyVerified.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(alreadyVerified.getBody()).containsEntry("code", "EMAIL_ALREADY_VERIFIED");
        assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertSingleAction("cho.xacminh", AdminActionType.SEND_VERIFICATION);
    }

    private void assertSingleAction(String username, AdminActionType type) {
        UUID targetId = userRepository.findByUsername(username).orElseThrow().getId();
        assertThat(adminActionRepository.findAll())
                .filteredOn(action -> targetId.equals(action.getTargetUserId()))
                .singleElement()
                .satisfies(action -> assertThat(action.getAction()).isEqualTo(type));
    }

    private String signInAsAdmin(String username, String email) throws Exception {
        registerVerified(username, email);
        userRepository
                .findByUsername(username)
                .ifPresent(
                        admin -> {
                            admin.setRole(Role.ADMIN);
                            userRepository.save(admin);
                        });
        return (String)
                restTemplate
                        .postForEntity(
                                "/api/auth/login",
                                Map.of("email", email, "password", PASSWORD, "rememberMe", false),
                                Map.class)
                        .getBody()
                        .get("accessToken");
    }

    private void registerVerified(String username, String email) throws Exception {
        restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username", username,
                        "email", email,
                        "password", PASSWORD,
                        "acceptedTerms", true),
                Map.class);
        String code =
                MailpitContainer.firstGroup(
                        MailpitContainer.CODE, MailpitContainer.client().awaitMailTo(email).text());
        restTemplate.postForEntity(
                "/api/auth/email-verification/confirm",
                Map.of("email", email, "code", code),
                Map.class);
    }

    private ResponseEntity<Map> post(String path, String adminToken) {
        return restTemplate.exchange(
                path, HttpMethod.POST, new HttpEntity<>(bearer(adminToken)), Map.class);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
    }
}
