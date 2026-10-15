package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.UserSession;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.auth.service.SecureTokens;
import java.net.HttpCookie;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LoginIntegrationTest extends MySqlContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserSessionRepository userSessionRepository;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        MailpitContainer.registerMailProperties(registry);
    }

    @Test
    @DisplayName("Xác thực mã xong là đăng nhập: token dùng được cho /me, cookie là cookie phiên")
    void confirmCode_signsInAndTokenWorksForMe() throws Exception {
        String email = "vao.thang@gmail.com";
        String code = registerAndReadCode("vao.thang", email);

        ResponseEntity<Map> confirmed =
                restTemplate.postForEntity(
                        "/api/auth/email-verification/confirm",
                        Map.of("email", email, "code", code),
                        Map.class);

        assertThat(confirmed.getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpCookie refreshCookie = refreshCookie(confirmed);
        assertThat(refreshCookie.getMaxAge()).isEqualTo(-1);
        ResponseEntity<Map> me = me((String) confirmed.getBody().get("accessToken"));
        assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(me.getBody()).containsEntry("username", "vao.thang");
        assertThat(me.getBody()).containsEntry("role", "LEARNER");
    }

    @Test
    @DisplayName(
            "Đăng nhập bằng mật khẩu sau khi xác thực: lưu phiên với hash, cookie sống 14 ngày")
    void login_verifiedUser_createsPersistentSession() throws Exception {
        String email = "dang.nhap@gmail.com";
        String code = registerAndReadCode("dang.nhap", email);
        restTemplate.postForEntity(
                "/api/auth/email-verification/confirm",
                Map.of("email", email, "code", code),
                Map.class);

        ResponseEntity<Map> login = login("DANG.NHAP@gmail.com", PASSWORD, true);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpCookie refreshCookie = refreshCookie(login);
        assertThat(refreshCookie.getMaxAge()).isEqualTo(14L * 24 * 60 * 60);
        List<UserSession> sessions =
                userSessionRepository.findAll().stream()
                        .filter(
                                session ->
                                        session.getTokenHash()
                                                .equals(
                                                        SecureTokens.sha256(
                                                                refreshCookie.getValue())))
                        .toList();
        assertThat(sessions)
                .singleElement()
                .satisfies(session -> assertThat(session.isPersistent()).isTrue());
        assertThat(me((String) login.getBody().get("accessToken")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Chưa xác thực email: 403 EMAIL_NOT_VERIFIED; sai mật khẩu: 401")
    void login_unverifiedOrWrongPassword_isRejected() throws Exception {
        String email = "chua.xac.thuc@gmail.com";
        registerAndReadCode("chua.xac.thuc", email);

        ResponseEntity<Map> unverified = login(email, PASSWORD, false);
        ResponseEntity<Map> wrongPassword = login(email, "sai-mat-khau-roi", false);

        assertThat(unverified.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(unverified.getBody()).containsEntry("code", "EMAIL_NOT_VERIFIED");
        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getBody()).containsEntry("code", "INVALID_CREDENTIALS");
        assertThat(wrongPassword.getHeaders().get(HttpHeaders.SET_COOKIE)).isNull();
    }

    private String registerAndReadCode(String username, String email) throws Exception {
        restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username", username,
                        "email", email,
                        "password", PASSWORD,
                        "acceptedTerms", true),
                Map.class);
        return MailpitContainer.firstGroup(
                MailpitContainer.CODE, MailpitContainer.client().awaitMailTo(email).text());
    }

    private ResponseEntity<Map> login(String email, String password, boolean rememberMe) {
        return restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("email", email, "password", password, "rememberMe", rememberMe),
                Map.class);
    }

    private ResponseEntity<Map> me(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return restTemplate.exchange(
                "/api/auth/me", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private static HttpCookie refreshCookie(ResponseEntity<?> response) {
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("HttpOnly").contains("Secure").contains("SameSite=Lax");
        HttpCookie cookie = HttpCookie.parse(setCookie).get(0);
        assertThat(cookie.getName()).isEqualTo("sqljudge_refresh");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        return cookie;
    }
}
