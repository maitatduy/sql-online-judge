package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
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
class AdminAuditLogIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminActionRepository adminActionRepository;

    @Test
    @DisplayName(
            "Nhật ký: thao tác thật hiện mới nhất trước, lọc được theo đối tượng, người thực hiện,"
                    + " loại và thời gian, kể cả thao tác không có đối tượng")
    void auditLog_endToEnd() throws Exception {
        registerVerified("nk.admin", "nk.admin@gmail.com");
        promoteToAdmin("nk.admin");
        String adminToken = accessToken(login("nk.admin@gmail.com"));
        registerVerified("nk.muctieu", "nk.muctieu@gmail.com");
        registerVerified("nk.khac", "nk.khac@gmail.com");
        patch(
                adminToken,
                "/api/admin/users/nk.muctieu",
                Map.of("role", "SETTER", "reason", "Ra đề cho contest"));
        ban(adminToken, "nk.muctieu", "Spam bình luận");
        ban(adminToken, "nk.khac", "Tài khoản phụ");
        UUID adminId = userRepository.findByUsername("nk.admin").orElseThrow().getId();
        adminActionRepository.save(
                AdminAction.exportUserEmails(adminId, "Đối soát danh sách", "12 tài khoản"));

        List<Map<String, Object>> onTarget = items(get(adminToken, "?user=nk.muctieu"));
        assertThat(onTarget)
                .extracting("action", "actorUsername", "actorRole", "targetUsername", "reason")
                .containsExactly(
                        tuple("BAN", "nk.admin", "ADMIN", "nk.muctieu", "Spam bình luận"),
                        tuple(
                                "UPDATE_ROLE",
                                "nk.admin",
                                "ADMIN",
                                "nk.muctieu",
                                "Ra đề cho contest"));
        assertThat(onTarget.get(0).get("expiresAt")).isNotNull();
        assertThat(onTarget.get(1))
                .containsEntry("previousValue", "LEARNER")
                .containsEntry("newValue", "SETTER");

        assertThat(items(get(adminToken, "?actor=nk.admin&action=BAN")))
                .extracting("targetUsername")
                .containsExactly("nk.khac", "nk.muctieu");
        assertThat(items(get(adminToken, "?actor=nk.admin&action=EXPORT_USER_EMAILS")))
                .singleElement()
                .satisfies(
                        export -> {
                            assertThat(export.get("targetUsername")).isNull();
                            assertThat(export).containsEntry("details", "12 tài khoản");
                        });
        String tomorrow = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        assertThat(items(get(adminToken, "?actor=nk.admin&from=" + tomorrow))).isEmpty();

        ResponseEntity<Map> firstPage = get(adminToken, "?actor=nk.admin&size=2");
        assertThat(firstPage.getBody())
                .containsEntry("totalItems", 4)
                .containsEntry("totalPages", 2);

        assertThat(items(get(adminToken, "?actor=nk.admin&q=khoản phụ")))
                .extracting("targetUsername")
                .containsExactly("nk.khac");
        assertThat(items(get(adminToken, "?actor=nk.admin&q=MUCTIEU")))
                .extracting("action")
                .containsExactly("BAN", "UPDATE_ROLE");
        assertThat(items(get(adminToken, "?actor=nk.admin&q=%25"))).isEmpty();
        assertThat(items(get(adminToken, "?actor=nk.admin&category=ACCOUNT"))).hasSize(4);
        assertThat(items(get(adminToken, "?actor=nk.admin&category=PROBLEM"))).isEmpty();

        ResponseEntity<Map> actors =
                restTemplate.exchange(
                        "/api/admin/audit-log/actors",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(adminToken)),
                        Map.class);
        assertThat((List<String>) actors.getBody().get("usernames")).contains("nk.admin");

        ResponseEntity<String> export =
                restTemplate.exchange(
                        "/api/admin/audit-log/export?actor=nk.admin&category=ACCOUNT",
                        HttpMethod.GET,
                        new HttpEntity<>(bearer(adminToken)),
                        String.class);
        assertThat(export.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(export.getHeaders().getContentDisposition().getFilename())
                .startsWith("audit-log-")
                .endsWith(".csv");
        String[] lines = export.getBody().split("\n");
        assertThat(lines).hasSize(5);
        assertThat(lines[0]).startsWith("occurredAt,actor,actorRole,action,target");
        assertThat(export.getBody()).contains("nk.admin,ADMIN,BAN,nk.khac");
    }

    @Test
    @DisplayName("Người không phải ADMIN bị 403, chưa đăng nhập bị 401")
    void auditLog_nonAdmin_isRejected() throws Exception {
        registerVerified("nk.hocvien", "nk.hocvien@gmail.com");
        String learnerToken = accessToken(login("nk.hocvien@gmail.com"));

        assertThat(get(learnerToken, "").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(restTemplate.getForEntity("/api/admin/audit-log", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private static List<Map<String, Object>> items(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (List<Map<String, Object>>) response.getBody().get("items");
    }

    private void ban(String adminToken, String username, String reason) {
        HttpHeaders headers = bearer(adminToken);
        restTemplate.postForEntity(
                "/api/admin/users/" + username + "/ban",
                new HttpEntity<>(
                        Map.of("duration", "DAYS_7", "reason", reason, "notifyByEmail", false),
                        headers),
                Map.class);
    }

    private void patch(String adminToken, String path, Map<String, ?> body) {
        restTemplate.exchange(
                path, HttpMethod.PATCH, new HttpEntity<>(body, bearer(adminToken)), Map.class);
    }

    private ResponseEntity<Map> get(String token, String query) {
        return restTemplate.exchange(
                "/api/admin/audit-log" + query,
                HttpMethod.GET,
                new HttpEntity<>(bearer(token)),
                Map.class);
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
