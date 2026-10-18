package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.net.HttpCookie;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminUserBanIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";
    private static final String BAN_SUBJECT = "Tài khoản SQL Judge đã bị khoá";
    private static final String UNBAN_SUBJECT = "Tài khoản SQL Judge đã được mở khoá";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminActionRepository adminActionRepository;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        MailpitContainer.registerMailProperties(registry);
    }

    @Test
    @DisplayName(
            "Khoá: người dùng bị đăng xuất, không đăng nhập được, nhận mail; mở khoá thì vào lại"
                    + " được; cả hai được ghi nhật ký")
    void banAndUnban_endToEnd() throws Exception {
        String adminEmail = "admin.khoa@gmail.com";
        signUp("admin.khoa", adminEmail);
        promoteToAdmin("admin.khoa");
        String adminToken = accessToken(login(adminEmail));
        String victimEmail = "nguoi.bi.khoa@gmail.com";
        String victimRefreshToken = signUp("nguoi.bi.khoa", victimEmail);

        ResponseEntity<Map> banned =
                post(
                        adminToken,
                        "/api/admin/users/nguoi.bi.khoa/ban",
                        Map.of(
                                "duration",
                                "DAYS_7",
                                "reason",
                                "Spam bình luận",
                                "notifyByEmail",
                                true));
        ResponseEntity<Map> refreshAfterBan = refresh(victimRefreshToken);
        ResponseEntity<Map> loginWhileBanned = login(victimEmail);
        String banMail =
                MailpitContainer.client().awaitMailWithSubject(victimEmail, BAN_SUBJECT).text();
        ResponseEntity<Map> unbanned =
                post(
                        adminToken,
                        "/api/admin/users/nguoi.bi.khoa/unban",
                        Map.of("reason", "Khoá nhầm"));
        String unbanMail =
                MailpitContainer.client().awaitMailWithSubject(victimEmail, UNBAN_SUBJECT).text();

        assertThat(banned.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(banned.getBody()).containsEntry("status", "BANNED");
        assertThat(banned.getBody().get("bannedUntil")).isNotNull();
        assertThat(refreshAfterBan.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(loginWhileBanned.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(loginWhileBanned.getBody()).containsEntry("code", "ACCOUNT_BANNED");
        assertThat(banMail).contains("Spam bình luận");
        assertThat(unbanned.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unbanned.getBody()).containsEntry("status", "ACTIVE");
        assertThat(unbanMail).contains("http://localhost:3000/login").doesNotContain("Khoá nhầm");
        assertThat(login(victimEmail).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(recordedActions("nguoi.bi.khoa"))
                .extracting(AdminAction::getAction, AdminAction::getReason)
                .containsExactlyInAnyOrder(
                        tuple(AdminActionType.BAN, "Spam bình luận"),
                        tuple(AdminActionType.UNBAN, "Khoá nhầm"));
    }

    @Test
    @DisplayName("Admin không tự khoá được chính mình: 422 CANNOT_MODIFY_SELF")
    void ban_self_returnsUnprocessable() throws Exception {
        String adminEmail = "admin.tu.khoa@gmail.com";
        signUp("admin.tu.khoa", adminEmail);
        promoteToAdmin("admin.tu.khoa");
        String adminToken = accessToken(login(adminEmail));

        ResponseEntity<Map> response =
                post(
                        adminToken,
                        "/api/admin/users/admin.tu.khoa/ban",
                        Map.of("duration", "PERMANENT", "reason", "Thử", "notifyByEmail", false));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).containsEntry("code", "CANNOT_MODIFY_SELF");
    }

    private String signUp(String username, String email) throws Exception {
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
        ResponseEntity<Map> confirmed =
                restTemplate.postForEntity(
                        "/api/auth/email-verification/confirm",
                        Map.of("email", email, "code", code),
                        Map.class);
        return HttpCookie.parse(confirmed.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .get(0)
                .getValue();
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

    private List<AdminAction> recordedActions(String username) {
        UUID targetId = userRepository.findByUsername(username).orElseThrow().getId();
        return adminActionRepository.findAll().stream()
                .filter(action -> action.getTargetUserId().equals(targetId))
                .toList();
    }

    private ResponseEntity<Map> post(String adminToken, String path, Map<String, ?> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return restTemplate.postForEntity(path, new HttpEntity<>(body, headers), Map.class);
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

    private ResponseEntity<Map> refresh(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ORIGIN, "http://localhost:3000");
        headers.add(HttpHeaders.COOKIE, "sqljudge_refresh=" + refreshToken);
        return restTemplate.exchange(
                "/api/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    }
}
