package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminUserSessionIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";
    private static final String CHROME_ON_WINDOWS =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko)"
                    + " Chrome/129.0.0.0 Safari/537.36";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminActionRepository adminActionRepository;

    @Test
    @DisplayName(
            "Admin thấy phiên đăng nhập của người dùng và đăng xuất được phiên đó; refresh cũ hết"
                    + " dùng được")
    void listAndRevokeSession_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.phien", "admin.phien@gmail.com");
        String email = "co.phien@gmail.com";
        registerVerified("co.phien", email);
        String refreshToken = refreshCookie(login(email, CHROME_ON_WINDOWS));
        String sessionsPath = "/api/admin/users/co.phien/sessions";

        ResponseEntity<Map> listed = exchange(HttpMethod.GET, sessionsPath, adminToken);
        List<Map<String, Object>> items = (List<Map<String, Object>>) listed.getBody().get("items");
        String sessionId = (String) items.get(0).get("id");
        ResponseEntity<Map> revoked =
                exchange(HttpMethod.DELETE, sessionsPath + "/" + sessionId, adminToken);
        ResponseEntity<Map> revokedAgain =
                exchange(HttpMethod.DELETE, sessionsPath + "/" + sessionId, adminToken);
        ResponseEntity<Map> afterRevoke = exchange(HttpMethod.GET, sessionsPath, adminToken);

        assertThat(listed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(items).hasSize(2);
        assertThat(items.get(0))
                .containsEntry("browser", "Chrome")
                .containsEntry("os", "Windows")
                .containsEntry("isActiveNow", true);
        assertThat(revoked.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(revokedAgain.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(revokedAgain.getBody()).containsEntry("code", "SESSION_NOT_FOUND");
        assertThat((List<Map<String, Object>>) afterRevoke.getBody().get("items"))
                .extracting(item -> item.get("id"))
                .containsExactly(items.get(1).get("id"));
        assertThat(refresh(refreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        UUID targetId = userRepository.findByUsername("co.phien").orElseThrow().getId();
        assertThat(adminActionRepository.findAll())
                .filteredOn(action -> action.getTargetUserId().equals(targetId))
                .singleElement()
                .satisfies(
                        action -> {
                            assertThat(action.getAction())
                                    .isEqualTo(AdminActionType.REVOKE_SESSION);
                            assertThat(action.getReason()).isNull();
                        });
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
        return (String) login(email, CHROME_ON_WINDOWS).getBody().get("accessToken");
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

    private ResponseEntity<Map> login(String email, String userAgent) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, userAgent);
        return restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(
                        Map.of("email", email, "password", PASSWORD, "rememberMe", false), headers),
                Map.class);
    }

    private static String refreshCookie(ResponseEntity<Map> login) {
        return HttpCookie.parse(login.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .get(0)
                .getValue();
    }

    private ResponseEntity<Map> exchange(HttpMethod method, String path, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return restTemplate.exchange(path, method, new HttpEntity<>(headers), Map.class);
    }

    private ResponseEntity<Map> refresh(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ORIGIN, "http://localhost:3000");
        headers.add(HttpHeaders.COOKIE, "sqljudge_refresh=" + refreshToken);
        return restTemplate.exchange(
                "/api/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    }
}
