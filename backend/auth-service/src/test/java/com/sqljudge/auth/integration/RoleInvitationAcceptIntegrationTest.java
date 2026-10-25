package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.repository.UserRepository;
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
class RoleInvitationAcceptIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";
    private static final Pattern INVITATION_LINK =
            Pattern.compile("http://localhost:3000/invitations/([A-Za-z0-9_-]{43})");

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName(
            "Nhận lời mời: ai cũng xem được, sai email bị chặn, đúng email thì thành Người ra đề,"
                    + " nhận lần hai báo đã nhận, có ghi nhật ký")
    void accept_endToEnd() throws Exception {
        registerVerified("nh.admin", "nh.admin@gmail.com");
        promoteToAdmin("nh.admin");
        String adminToken = accessToken(login("nh.admin@gmail.com"));
        registerVerified("nh.hocvien", "nh.hocvien@gmail.com");
        registerVerified("nh.khac", "nh.khac@gmail.com");
        restTemplate.postForEntity(
                "/api/admin/role-invitations",
                new HttpEntity<>(
                        Map.of("emails", List.of("nh.hocvien@gmail.com"), "role", "SETTER"),
                        bearer(adminToken)),
                Map.class);
        String token =
                MailpitContainer.firstGroup(
                        INVITATION_LINK,
                        MailpitContainer.client()
                                .awaitMailWithSubject(
                                        "nh.hocvien@gmail.com",
                                        "Lời mời tham gia đội ngũ SQL Judge")
                                .text());
        String path = "/api/role-invitations/" + token;

        ResponseEntity<Map> view = restTemplate.getForEntity(path, Map.class);
        assertThat(view.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(view.getBody())
                .containsEntry("maskedEmail", "nh***@gmail.com")
                .containsEntry("role", "SETTER")
                .containsEntry("inviterUsername", "nh.admin")
                .containsEntry("status", "PENDING");

        ResponseEntity<Map> wrongPerson = accept(path, accessToken(login("nh.khac@gmail.com")));
        assertThat(wrongPerson.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(wrongPerson.getBody()).containsEntry("code", "INVITATION_EMAIL_MISMATCH");

        String inviteeToken = accessToken(login("nh.hocvien@gmail.com"));
        ResponseEntity<Map> accepted = accept(path, inviteeToken);
        assertThat(accepted.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(accepted.getBody()).containsEntry("role", "SETTER");
        assertThat(userRepository.findByUsername("nh.hocvien").orElseThrow().getRole())
                .isEqualTo(Role.SETTER);
        assertThat((Map<String, Object>) login("nh.hocvien@gmail.com").getBody().get("user"))
                .containsEntry("role", "SETTER");

        ResponseEntity<Map> again = accept(path, inviteeToken);
        assertThat(again.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(again.getBody()).containsEntry("code", "INVITATION_ALREADY_ACCEPTED");
        assertThat(restTemplate.getForEntity(path, Map.class).getBody())
                .containsEntry("status", "ACCEPTED");

        ResponseEntity<Map> logged =
                restTemplate.exchange(
                        "/api/admin/audit-log?user=nh.hocvien&action=ACCEPT_ROLE_INVITATION",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(adminToken)),
                        Map.class);
        assertThat((List<Map<String, Object>>) logged.getBody().get("items"))
                .singleElement()
                .satisfies(
                        entry ->
                                assertThat(entry)
                                        .containsEntry("actorUsername", "nh.hocvien")
                                        .containsEntry("previousValue", "LEARNER")
                                        .containsEntry("newValue", "SETTER"));
    }

    @Test
    @DisplayName("Token không tồn tại: 404 khi xem và khi nhận")
    void unknownToken_isNotFound() throws Exception {
        registerVerified("nh.lac", "nh.lac@gmail.com");
        String path = "/api/role-invitations/khong-co-token-nay";

        assertThat(restTemplate.getForEntity(path, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(accept(path, accessToken(login("nh.lac@gmail.com"))).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Map> accept(String path, String token) {
        return restTemplate.postForEntity(
                path + "/accept", new HttpEntity<>(bearer(token)), Map.class);
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
