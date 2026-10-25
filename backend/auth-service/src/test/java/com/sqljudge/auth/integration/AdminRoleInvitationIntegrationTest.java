package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.sqljudge.auth.entity.RoleInvitation;
import com.sqljudge.auth.repository.RoleInvitationRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.SecureTokens;
import com.sqljudge.common.security.Role;
import java.util.List;
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
class AdminRoleInvitationIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";
    private static final Pattern INVITATION_LINK =
            Pattern.compile("http://localhost:3000/invitations/([A-Za-z0-9_-]{43})");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleInvitationRepository roleInvitationRepository;

    @Test
    @DisplayName(
            "Mời hàng loạt: gửi mail có link riêng, bỏ qua người đã có vai trò, mời lại thì báo"
                    + " đang chờ, mỗi lời mời ghi một dòng nhật ký")
    void invite_endToEnd() throws Exception {
        registerVerified("mo.admin", "mo.admin@gmail.com");
        promote("mo.admin", Role.ADMIN);
        String adminToken = accessToken(login("mo.admin@gmail.com"));
        registerVerified("mo.hocvien", "mo.hocvien@gmail.com");
        registerVerified("mo.rade", "mo.rade@gmail.com");
        promote("mo.rade", Role.SETTER);

        ResponseEntity<Map> response =
                invite(
                        adminToken,
                        List.of(
                                "mo.hocvien@gmail.com",
                                "Mo.Chua.Co@gmail.com",
                                "mo.rade@gmail.com"),
                        "Ra đề cho SQL Weekly");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("sentCount", 2);
        assertThat((List<Map<String, String>>) response.getBody().get("skipped"))
                .extracting("email", "reason")
                .containsExactly(tuple("mo.rade@gmail.com", "Đã có vai trò này"));

        String mail = MailpitContainer.client().awaitMailTo("mo.chua.co@gmail.com").text();
        assertThat(mail)
                .contains("mo.admin")
                .contains("Người ra đề")
                .contains("Ra đề cho SQL Weekly");
        String token = MailpitContainer.firstGroup(INVITATION_LINK, mail);
        RoleInvitation stored =
                roleInvitationRepository.findAll().stream()
                        .filter(invitation -> invitation.getEmail().equals("mo.chua.co@gmail.com"))
                        .findFirst()
                        .orElseThrow();
        assertThat(stored.getTokenHash()).isEqualTo(SecureTokens.sha256(token)).isNotEqualTo(token);

        ResponseEntity<Map> again = invite(adminToken, List.of("mo.hocvien@gmail.com"), null);
        assertThat(again.getBody()).containsEntry("sentCount", 0);
        assertThat((List<Map<String, String>>) again.getBody().get("skipped"))
                .extracting("reason")
                .containsExactly("Đang có lời mời chờ nhận");

        ResponseEntity<Map> logged =
                restTemplate.exchange(
                        "/api/admin/audit-log?actor=mo.admin&action=INVITE_ROLE",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(adminToken)),
                        Map.class);
        assertThat((List<Map<String, Object>>) logged.getBody().get("items"))
                .extracting("targetUsername", "newValue", "details")
                .containsExactlyInAnyOrder(
                        tuple("mo.hocvien", "SETTER", null),
                        tuple(null, "SETTER", "mo.chua.co@gmail.com"));
    }

    @Test
    @DisplayName("Người không phải ADMIN bị 403")
    void invite_nonAdmin_isForbidden() throws Exception {
        registerVerified("mo.thuong", "mo.thuong@gmail.com");
        String learnerToken = accessToken(login("mo.thuong@gmail.com"));

        assertThat(invite(learnerToken, List.of("x@gmail.com"), null).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    private ResponseEntity<Map> invite(String token, List<String> emails, String message) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("emails", emails);
        body.put("role", "SETTER");
        body.put("message", message);
        return restTemplate.postForEntity(
                "/api/admin/role-invitations", new HttpEntity<>(body, bearer(token)), Map.class);
    }

    private static HttpHeaders bearer(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return headers;
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

    private void promote(String username, Role role) {
        userRepository
                .findByUsername(username)
                .ifPresent(
                        user -> {
                            user.setRole(role);
                            userRepository.save(user);
                        });
    }

    private ResponseEntity<Map> login(String email) {
        return restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("email", email, "password", PASSWORD, "rememberMe", false),
                Map.class);
    }

    private static String accessToken(ResponseEntity<Map> login) {
        return (String) login.getBody().get("accessToken");
    }
}
