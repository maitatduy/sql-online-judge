package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.net.HttpCookie;
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
class AdminUserEditIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminActionRepository adminActionRepository;

    @Test
    @DisplayName(
            "Đổi vai trò: người dùng bị đăng xuất, đăng nhập lại nhận vai trò mới, nhật ký ghi cũ"
                    + " → mới")
    void changeRole_endToEnd() throws Exception {
        registerVerified("admin.sua", "admin.sua@gmail.com");
        promoteToAdmin("admin.sua");
        String adminToken = accessToken(login("admin.sua@gmail.com"));
        String email = "duoc.nang@gmail.com";
        registerVerified("duoc.nang", email);
        String refreshToken = refreshCookie(login(email));

        ResponseEntity<Map> updated =
                patch(
                        adminToken,
                        "/api/admin/users/duoc.nang",
                        Map.of("role", "SETTER", "reason", "Ra đề cho contest"));
        ResponseEntity<Map> refreshAfterChange = refresh(refreshToken);
        ResponseEntity<Map> loginAgain = login(email);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).containsEntry("role", "SETTER");
        assertThat(refreshAfterChange.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat((Map<String, Object>) loginAgain.getBody().get("user"))
                .containsEntry("role", "SETTER");
        UUID targetId = userRepository.findByUsername("duoc.nang").orElseThrow().getId();
        assertThat(adminActionRepository.findAll())
                .filteredOn(action -> targetId.equals(action.getTargetUserId()))
                .singleElement()
                .satisfies(
                        action -> {
                            assertThat(action.getAction()).isEqualTo(AdminActionType.UPDATE_ROLE);
                            assertThat(action.getPreviousValue()).isEqualTo("LEARNER");
                            assertThat(action.getNewValue()).isEqualTo("SETTER");
                            assertThat(action.getReason()).isEqualTo("Ra đề cho contest");
                        });
    }

    @Test
    @DisplayName("Đổi vai trò thiếu lý do: 400 ở trường reason")
    void changeRole_withoutReason_returnsFieldError() throws Exception {
        registerVerified("admin.thieu.ly.do", "admin.thieu.ly.do@gmail.com");
        promoteToAdmin("admin.thieu.ly.do");
        String adminToken = accessToken(login("admin.thieu.ly.do@gmail.com"));
        registerVerified("chua.doi", "chua.doi@gmail.com");

        ResponseEntity<Map> response =
                patch(adminToken, "/api/admin/users/chua.doi", Map.of("role", "SETTER"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("code", "VALIDATION_FAILED");
        assertThat(response.getBody().toString()).contains("field=reason");
    }

    @Test
    @DisplayName(
            "Hạ vai trò quản trị viên khác: 422 CANNOT_MODIFY_ADMIN, vẫn là ADMIN, không ghi nhật"
                    + " ký")
    void changeRole_otherAdmin_returnsUnprocessable() throws Exception {
        registerVerified("admin.ha.quyen", "admin.ha.quyen@gmail.com");
        promoteToAdmin("admin.ha.quyen");
        String adminToken = accessToken(login("admin.ha.quyen@gmail.com"));
        registerVerified("admin.bi.ha", "admin.bi.ha@gmail.com");
        promoteToAdmin("admin.bi.ha");

        ResponseEntity<Map> response =
                patch(
                        adminToken,
                        "/api/admin/users/admin.bi.ha",
                        Map.of("role", "LEARNER", "reason", "Hạ để khoá"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).containsEntry("code", "CANNOT_MODIFY_ADMIN");
        var target = userRepository.findByUsername("admin.bi.ha").orElseThrow();
        assertThat(target.getRole()).isEqualTo(Role.ADMIN);
        assertThat(adminActionRepository.findAll())
                .noneMatch(action -> target.getId().equals(action.getTargetUserId()));
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

    private static String refreshCookie(ResponseEntity<Map> login) {
        return HttpCookie.parse(login.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .get(0)
                .getValue();
    }

    private ResponseEntity<Map> patch(String adminToken, String path, Map<String, ?> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return restTemplate.exchange(
                path, HttpMethod.PATCH, new HttpEntity<>(body, headers), Map.class);
    }

    private ResponseEntity<Map> refresh(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ORIGIN, "http://localhost:3000");
        headers.add(HttpHeaders.COOKIE, "sqljudge_refresh=" + refreshToken);
        return restTemplate.exchange(
                "/api/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    }
}
