package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminUserDeletionIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";
    private static final String REASON = "Tài khoản spam";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminActionRepository adminActionRepository;

    @Test
    @DisplayName(
            "Admin xoá tài khoản: 204, không đăng nhập hay refresh được nữa, chỉ hiện khi lọc đã"
                    + " xoá")
    void delete_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.xoa", "admin.xoa@gmail.com");
        String email = "se.bi.xoa@gmail.com";
        registerVerified("se.bi.xoa", email);
        String refreshToken = refreshCookie(login(email));

        DeleteResult deleted = deleteUser("se.bi.xoa", adminToken);
        DeleteResult deletedAgain = deleteUser("se.bi.xoa", adminToken);

        assertThat(deleted.status()).isEqualTo(HttpStatus.NO_CONTENT.value());
        assertThat(deletedAgain.status()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(login(email).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(refreshToken).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(usernamesIn("/api/admin/users?q=se.bi.xoa", adminToken)).isEmpty();
        assertThat(usernamesIn("/api/admin/users?q=se.bi.xoa&deleted=true", adminToken))
                .containsExactly("se.bi.xoa");
        UUID targetId = userRepository.findByUsername("se.bi.xoa").orElseThrow().getId();
        assertThat(adminActionRepository.findAll())
                .filteredOn(action -> targetId.equals(action.getTargetUserId()))
                .singleElement()
                .satisfies(
                        action -> {
                            assertThat(action.getAction()).isEqualTo(AdminActionType.DELETE);
                            assertThat(action.getReason()).isEqualTo(REASON);
                        });
    }

    @Test
    @DisplayName("Admin tự xoá chính mình: 422 và tài khoản vẫn còn")
    void delete_self_rejected() throws Exception {
        String adminToken = signInAsAdmin("admin.tuxoa", "admin.tuxoa@gmail.com");

        DeleteResult rejected = deleteUser("admin.tuxoa", adminToken);

        assertThat(rejected.status()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(rejected.body()).containsEntry("code", "CANNOT_MODIFY_SELF");
        assertThat(userRepository.findByUsername("admin.tuxoa").orElseThrow().isDeleted())
                .isFalse();
    }

    @Test
    @DisplayName(
            "Admin khôi phục tài khoản đã xoá: 200, quay lại danh sách, đăng nhập lại được;"
                    + " khôi phục lần nữa bị 409")
    void restore_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.khoiphuc", "admin.khoiphuc@gmail.com");
        String email = "duoc.khoiphuc@gmail.com";
        registerVerified("duoc.khoiphuc", email);
        deleteUser("duoc.khoiphuc", adminToken);

        ResponseEntity<Map> restored = restore("duoc.khoiphuc", adminToken);
        ResponseEntity<Map> restoredAgain = restore("duoc.khoiphuc", adminToken);

        assertThat(restored.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(restored.getBody())
                .containsEntry("username", "duoc.khoiphuc")
                .containsEntry("deletedAt", null);
        assertThat(restoredAgain.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(restoredAgain.getBody()).containsEntry("code", "USER_NOT_DELETED");
        assertThat(usernamesIn("/api/admin/users?q=duoc.khoiphuc", adminToken))
                .containsExactly("duoc.khoiphuc");
        assertThat(login(email).getStatusCode()).isEqualTo(HttpStatus.OK);
        UUID targetId = userRepository.findByUsername("duoc.khoiphuc").orElseThrow().getId();
        assertThat(adminActionRepository.findAll())
                .filteredOn(action -> targetId.equals(action.getTargetUserId()))
                .extracting(action -> action.getAction())
                .containsExactlyInAnyOrder(AdminActionType.DELETE, AdminActionType.RESTORE);
    }

    private ResponseEntity<Map> restore(String username, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return restTemplate.exchange(
                "/api/admin/users/" + username + "/restore",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Map.class);
    }

    private DeleteResult deleteUser(String username, String adminToken)
            throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(Map.of("reason", REASON));
        HttpRequest request =
                HttpRequest.newBuilder(
                                URI.create(
                                        restTemplate.getRootUri() + "/api/admin/users/" + username))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .method("DELETE", HttpRequest.BodyPublishers.ofString(body))
                        .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        Map<String, Object> parsed =
                response.body().isEmpty()
                        ? Map.of()
                        : objectMapper.readValue(response.body(), Map.class);
        return new DeleteResult(response.statusCode(), parsed);
    }

    private List<Object> usernamesIn(String path, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        ResponseEntity<Map> page =
                restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        return ((List<Map<String, Object>>) page.getBody().get("items"))
                .stream().map(item -> item.get("username")).toList();
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
        return (String) login(email).getBody().get("accessToken");
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

    private ResponseEntity<Map> login(String email) {
        return restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("email", email, "password", PASSWORD, "rememberMe", false),
                Map.class);
    }

    private static String refreshCookie(ResponseEntity<Map> login) {
        return HttpCookie.parse(login.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .get(0)
                .getValue();
    }

    private ResponseEntity<Map> refresh(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ORIGIN, "http://localhost:3000");
        headers.add(HttpHeaders.COOKIE, "sqljudge_refresh=" + refreshToken);
        return restTemplate.exchange(
                "/api/auth/refresh", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
    }

    private record DeleteResult(int status, Map<String, Object> body) {}
}
