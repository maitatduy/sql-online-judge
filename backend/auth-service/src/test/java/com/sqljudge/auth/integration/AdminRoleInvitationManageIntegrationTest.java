package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.time.Duration;
import java.time.Instant;
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
class AdminRoleInvitationManageIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";
    private static final String INVITATION_SUBJECT = "Lời mời tham gia đội ngũ SQL Judge";
    private static final Pattern INVITATION_LINK =
            Pattern.compile("http://localhost:3000/invitations/([A-Za-z0-9_-]{43})");
    private static final Duration NEW_MAIL_TIMEOUT = Duration.ofSeconds(10);

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName(
            "Quản lý lời mời: lọc theo trạng thái và email, huỷ làm link hết dùng được, gửi lại"
                    + " cấp link mới và vô hiệu link cũ")
    void manage_endToEnd() throws Exception {
        registerVerified("ql.admin", "ql.admin@gmail.com");
        promoteToAdmin("ql.admin");
        String adminToken = accessToken(login("ql.admin@gmail.com"));
        post(
                adminToken,
                "/api/admin/role-invitations",
                Map.of(
                        "emails",
                        List.of("ql.huy@gmail.com", "ql.gui.lai@gmail.com"),
                        "role",
                        "SETTER"));
        String revokedToken = invitationToken("ql.huy@gmail.com");
        String oldToken = invitationToken("ql.gui.lai@gmail.com");

        List<Map<String, Object>> pending = items(get(adminToken, "?status=PENDING&q=ql."));
        assertThat(pending)
                .extracting("email")
                .containsExactlyInAnyOrder("ql.huy@gmail.com", "ql.gui.lai@gmail.com");
        assertThat(pending)
                .allSatisfy(item -> assertThat(item).containsEntry("inviterUsername", "ql.admin"));
        String revokedId = idOf(pending, "ql.huy@gmail.com");
        String resentId = idOf(pending, "ql.gui.lai@gmail.com");

        assertThat(post(adminToken, revokePath(revokedId), null).getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(items(get(adminToken, "?status=REVOKED&q=ql.huy")))
                .singleElement()
                .satisfies(item -> assertThat(item).containsEntry("status", "REVOKED"));
        assertThat(view(revokedToken).getBody()).containsEntry("status", "REVOKED");
        ResponseEntity<Map> revokeAgain = post(adminToken, revokePath(revokedId), null);
        assertThat(revokeAgain.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(revokeAgain.getBody()).containsEntry("code", "INVITATION_NOT_PENDING");

        ResponseEntity<Map> resent =
                post(adminToken, "/api/admin/role-invitations/" + resentId + "/resend", null);
        assertThat(resent.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resent.getBody()).containsEntry("status", "PENDING");
        String newToken = awaitNewToken("ql.gui.lai@gmail.com", oldToken);
        assertThat(view(oldToken).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(view(newToken).getBody()).containsEntry("status", "PENDING");

        ResponseEntity<Map> logged =
                restTemplate.exchange(
                        "/api/admin/audit-log?actor=ql.admin",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(adminToken)),
                        Map.class);
        assertThat((List<Map<String, Object>>) logged.getBody().get("items"))
                .extracting("action")
                .contains("REVOKE_ROLE_INVITATION", "RESEND_ROLE_INVITATION");
    }

    private String awaitNewToken(String email, String oldToken) throws Exception {
        Instant deadline = Instant.now().plus(NEW_MAIL_TIMEOUT);
        while (Instant.now().isBefore(deadline)) {
            String token = invitationToken(email);
            if (!token.equals(oldToken)) {
                return token;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("No new invitation mail for " + email);
    }

    private String invitationToken(String email) throws Exception {
        return MailpitContainer.firstGroup(
                INVITATION_LINK,
                MailpitContainer.client().awaitMailWithSubject(email, INVITATION_SUBJECT).text());
    }

    private static String idOf(List<Map<String, Object>> items, String email) {
        return items.stream()
                .filter(item -> email.equals(item.get("email")))
                .map(item -> (String) item.get("id"))
                .findFirst()
                .orElseThrow();
    }

    private static String revokePath(String id) {
        return "/api/admin/role-invitations/" + id + "/revoke";
    }

    private static List<Map<String, Object>> items(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (List<Map<String, Object>>) response.getBody().get("items");
    }

    private ResponseEntity<Map> view(String token) {
        return restTemplate.getForEntity("/api/role-invitations/" + token, Map.class);
    }

    private ResponseEntity<Map> get(String token, String query) {
        return restTemplate.exchange(
                "/api/admin/role-invitations" + query,
                HttpMethod.GET,
                new HttpEntity<>(bearer(token)),
                Map.class);
    }

    private ResponseEntity<Map> post(String token, String path, Object body) {
        return restTemplate.postForEntity(path, new HttpEntity<>(body, bearer(token)), Map.class);
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

    private void promoteToAdmin(String username) {
        userRepository
                .findByUsername(username)
                .ifPresent(
                        user -> {
                            user.setRole(Role.ADMIN);
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
