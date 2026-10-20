package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.sqljudge.auth.entity.AdminAction;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.net.HttpCookie;
import java.time.Instant;
import java.util.List;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminUserBulkIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminActionRepository adminActionRepository;

    @Test
    @DisplayName(
            "Đổi vai trò hàng loạt: đổi tài khoản hợp lệ, bỏ qua chính mình, admin, không tồn tại,"
                    + " ghi nhật ký cho từng tài khoản đã đổi")
    void bulkRole_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.loat", "admin.loat@gmail.com");
        saveUser("loat.mot", Role.LEARNER);
        saveUser("loat.hai", Role.MODERATOR);
        saveUser("loat.admin", Role.ADMIN);

        ResponseEntity<Map> response =
                post(
                        adminToken,
                        "/api/admin/users/bulk-role",
                        Map.of(
                                "usernames",
                                List.of(
                                        "loat.mot",
                                        "loat.hai",
                                        "loat.admin",
                                        "admin.loat",
                                        "loat.khong.co"),
                                "role",
                                "SETTER",
                                "reason",
                                "Mời cộng tác ra đề"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("affectedCount", 2);
        assertThat(response.getBody())
                .containsEntry(
                        "skippedUsernames", List.of("loat.admin", "admin.loat", "loat.khong.co"));
        assertThat(roleOf("loat.mot")).isEqualTo(Role.SETTER);
        assertThat(roleOf("loat.hai")).isEqualTo(Role.SETTER);
        assertThat(roleOf("loat.admin")).isEqualTo(Role.ADMIN);
        assertThat(recordedActions("loat.mot"))
                .extracting(AdminAction::getAction, AdminAction::getPreviousValue)
                .containsExactly(tuple(AdminActionType.UPDATE_ROLE, "LEARNER"));
        assertThat(recordedActions("loat.admin")).isEmpty();
    }

    @Test
    @DisplayName(
            "Khoá hàng loạt: khoá tài khoản hợp lệ, gửi email, ghi nhật ký; bỏ qua admin, đã bị"
                    + " khoá, chính mình")
    void bulkBan_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.khoaloat", "admin.khoaloat@gmail.com");
        saveUser("khoa.mot", Role.LEARNER);
        saveUser("khoa.hai", Role.SETTER);
        saveUser("khoa.admin", Role.ADMIN);
        saveUser("khoa.roi", Role.LEARNER);
        User alreadyBanned = userRepository.findByUsername("khoa.roi").orElseThrow();
        alreadyBanned.ban(Instant.now(), null, "Vi phạm cũ");
        userRepository.save(alreadyBanned);

        ResponseEntity<Map> response =
                post(
                        adminToken,
                        "/api/admin/users/bulk-ban",
                        Map.of(
                                "usernames",
                                List.of(
                                        "khoa.mot",
                                        "khoa.hai",
                                        "khoa.admin",
                                        "khoa.roi",
                                        "admin.khoaloat"),
                                "duration",
                                "DAYS_30",
                                "reason",
                                "Spam hàng loạt",
                                "notifyByEmail",
                                true));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("affectedCount", 2);
        assertThat(response.getBody())
                .containsEntry(
                        "skippedUsernames", List.of("khoa.admin", "khoa.roi", "admin.khoaloat"));
        assertThat(banReasonOf("khoa.mot")).isEqualTo("Spam hàng loạt");
        assertThat(banReasonOf("khoa.hai")).isEqualTo("Spam hàng loạt");
        assertThat(banReasonOf("khoa.admin")).isNull();
        assertThat(banReasonOf("khoa.roi")).isEqualTo("Vi phạm cũ");
        assertThat(recordedActions("khoa.mot"))
                .extracting(AdminAction::getAction)
                .containsExactly(AdminActionType.BAN);
        assertThat(MailpitContainer.client().awaitMailTo("khoa.hai@gmail.com").text())
                .contains("Spam hàng loạt");
    }

    private String banReasonOf(String username) {
        return userRepository.findByUsername(username).orElseThrow().getBanReason();
    }

    @Test
    @DisplayName(
            "Đổi vai trò hàng loạt: phiên của tài khoản được đổi bị thu hồi thật (refresh token cũ"
                    + " trả 401); bỏ qua tài khoản đã đúng vai trò và tài khoản đã xoá")
    void bulkRole_revokesSessionsAndSkipsUnchangedOrDeleted() throws Exception {
        String adminToken = signInAsAdmin("admin.phien", "admin.phien@gmail.com");
        String refreshToken = signUp("phien.mot", "phien.mot@gmail.com");
        saveUser("phien.setter", Role.SETTER);
        saveUser("phien.xoa", Role.LEARNER);
        User deleted = userRepository.findByUsername("phien.xoa").orElseThrow();
        deleted.markDeleted(Instant.now());
        userRepository.save(deleted);

        ResponseEntity<Map> response =
                post(
                        adminToken,
                        "/api/admin/users/bulk-role",
                        Map.of(
                                "usernames",
                                List.of("phien.mot", "phien.setter", "phien.xoa"),
                                "role",
                                "SETTER",
                                "reason",
                                "Mời ra đề"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("affectedCount", 1);
        assertThat(response.getBody())
                .containsEntry("skippedUsernames", List.of("phien.setter", "phien.xoa"));
        assertThat(roleOf("phien.mot")).isEqualTo(Role.SETTER);
        assertThat(roleOf("phien.xoa")).isEqualTo(Role.LEARNER);
        assertThat(recordedActions("phien.setter")).isEmpty();
        assertThat(refresh(refreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private Role roleOf(String username) {
        return userRepository.findByUsername(username).orElseThrow().getRole();
    }

    private void saveUser(String username, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@gmail.com");
        user.setRole(role);
        user.setSignupProvider(SignupProvider.EMAIL);
        user.setEmailVerifiedAt(Instant.now());
        userRepository.save(user);
    }

    private List<AdminAction> recordedActions(String username) {
        User target = userRepository.findByUsername(username).orElseThrow();
        return adminActionRepository.findAll().stream()
                .filter(action -> target.getId().equals(action.getTargetUserId()))
                .toList();
    }

    private ResponseEntity<Map> post(String adminToken, String path, Map<String, ?> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return restTemplate.postForEntity(path, new HttpEntity<>(body, headers), Map.class);
    }

    private ResponseEntity<Map> refresh(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ORIGIN, "http://localhost:3000");
        headers.add(HttpHeaders.COOKIE, "sqljudge_refresh=" + refreshToken);
        return restTemplate.exchange(
                "/api/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
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

    private String signInAsAdmin(String username, String email) throws Exception {
        signUp(username, email);
        User admin = userRepository.findByUsername(username).orElseThrow();
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        return (String)
                restTemplate
                        .postForEntity(
                                "/api/auth/login",
                                Map.of("email", email, "password", PASSWORD, "rememberMe", false),
                                Map.class)
                        .getBody()
                        .get("accessToken");
    }
}
