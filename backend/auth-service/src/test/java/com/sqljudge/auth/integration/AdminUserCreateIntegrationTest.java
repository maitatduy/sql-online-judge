package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminUserCreateIntegrationTest extends ContainerSupport {

    private static final Pattern PASSWORD_LINK =
            Pattern.compile("http://localhost:3000/reset-password\\?token=([A-Za-z0-9_-]{43})");
    private static final String INVITE_SUBJECT = "Kích hoạt tài khoản SQL Judge";
    private static final String ADMIN_PASSWORD = "matkhau-admin-2026";
    private static final String NEW_PASSWORD = "matkhau-moi-2026";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        MailpitContainer.registerMailProperties(registry);
    }

    @Test
    @DisplayName(
            "Admin tạo tài khoản: người dùng nhận mail kích hoạt, đặt mật khẩu và đăng nhập được")
    void createUser_inviteFlow_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.tao", "admin.tao@gmail.com");
        String email = "moi.tao@gmail.com";

        ResponseEntity<Map> created =
                createUser(
                        adminToken,
                        Map.of(
                                "username", "moi.tao",
                                "email", email,
                                "role", "SETTER",
                                "displayName", "Người Mới"));
        ResponseEntity<Map> loginBeforeActivation = login(email, NEW_PASSWORD);
        String token = readLinkToken(email, INVITE_SUBJECT);
        ResponseEntity<Map> activated = confirmPassword(token, NEW_PASSWORD);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(created.getHeaders().getLocation()).hasPath("/api/admin/users/moi.tao");
        assertThat(created.getBody())
                .containsEntry("username", "moi.tao")
                .containsEntry("role", "SETTER")
                .containsEntry("isEmailVerified", false)
                .containsEntry("signupProvider", "EMAIL");
        assertThat(loginBeforeActivation.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(activated.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(login(email, NEW_PASSWORD).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Người được mời đặt mật khẩu qua Quên mật khẩu: link mời cũ không dùng được nữa")
    void createUser_passwordSetByResetLink_invalidatesInviteLink() throws Exception {
        String adminToken = signInAsAdmin("admin.moi", "admin.moi@gmail.com");
        String email = "quen.loi.moi@gmail.com";
        createUser(
                adminToken, Map.of("username", "quen.loi.moi", "email", email, "role", "LEARNER"));
        String inviteToken = readLinkToken(email, INVITE_SUBJECT);

        restTemplate.postForEntity("/api/auth/password-reset", Map.of("email", email), Void.class);
        String resetToken = readLinkToken(email, "Đặt lại mật khẩu SQL Judge");
        ResponseEntity<Map> reset = confirmPassword(resetToken, NEW_PASSWORD);
        ResponseEntity<Map> lateInvite = confirmPassword(inviteToken, "matkhau-khac-2026");

        assertThat(reset.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(lateInvite.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(lateInvite.getBody()).containsEntry("code", "TOKEN_INVALID_OR_EXPIRED");
    }

    @Test
    @DisplayName("Username hoặc email đã có (email không phân biệt hoa thường): 409")
    void createUser_duplicate_returnsConflict() throws Exception {
        String adminToken = signInAsAdmin("admin.trung", "admin.trung@gmail.com");

        ResponseEntity<Map> sameUsername =
                createUser(
                        adminToken,
                        Map.of(
                                "username",
                                "admin.trung",
                                "email",
                                "khac@gmail.com",
                                "role",
                                "LEARNER"));
        ResponseEntity<Map> sameEmail =
                createUser(
                        adminToken,
                        Map.of(
                                "username", "ten.khac",
                                "email", "Admin.Trung@Gmail.com",
                                "role", "LEARNER"));

        assertThat(sameUsername.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sameUsername.getBody()).containsEntry("code", "USERNAME_TAKEN");
        assertThat(sameEmail.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(sameEmail.getBody()).containsEntry("code", "EMAIL_TAKEN");
    }

    private String signInAsAdmin(String username, String email) throws Exception {
        restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username", username,
                        "email", email,
                        "password", ADMIN_PASSWORD,
                        "acceptedTerms", true),
                Map.class);
        String code =
                MailpitContainer.firstGroup(
                        MailpitContainer.CODE, MailpitContainer.client().awaitMailTo(email).text());
        restTemplate.postForEntity(
                "/api/auth/email-verification/confirm",
                Map.of("email", email, "code", code),
                Map.class);
        userRepository
                .findByUsername(username)
                .ifPresent(
                        admin -> {
                            admin.setRole(Role.ADMIN);
                            userRepository.save(admin);
                        });
        return (String) login(email, ADMIN_PASSWORD).getBody().get("accessToken");
    }

    private ResponseEntity<Map> createUser(String adminToken, Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return restTemplate.postForEntity(
                "/api/admin/users", new HttpEntity<>(body, headers), Map.class);
    }

    private static String readLinkToken(String email, String subject) throws Exception {
        return MailpitContainer.firstGroup(
                PASSWORD_LINK,
                MailpitContainer.client().awaitMailWithSubject(email, subject).text());
    }

    private ResponseEntity<Map> confirmPassword(String token, String newPassword) {
        return restTemplate.postForEntity(
                "/api/auth/password-reset/confirm",
                Map.of("token", token, "newPassword", newPassword),
                Map.class);
    }

    private ResponseEntity<Map> login(String email, String password) {
        return restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("email", email, "password", password, "rememberMe", false),
                Map.class);
    }
}
