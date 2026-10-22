package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.sqljudge.auth.entity.AdminUserNote;
import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminUserNoteRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
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
class AdminUserNoteIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminUserNoteRepository adminUserNoteRepository;

    @Test
    @DisplayName(
            "Ghi chú: thêm hai ghi chú, xem lại mới nhất trước kèm tên người viết; tài khoản đã xoá"
                    + " vẫn xem được nhưng không thêm được")
    void notes_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.ghichu", "admin.ghichu@gmail.com");
        User target = saveUser("ghichu.mot");

        ResponseEntity<Map> first =
                post(adminToken, "/api/admin/users/ghichu.mot/notes", "  Lần 1  ");
        ResponseEntity<Map> second = post(adminToken, "/api/admin/users/ghichu.mot/notes", "Lần 2");
        ResponseEntity<Map> listed = get(adminToken, "/api/admin/users/ghichu.mot/notes");

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getHeaders().getLocation().getPath())
                .isEqualTo("/api/admin/users/ghichu.mot/notes/" + first.getBody().get("id"));
        assertThat(first.getBody())
                .containsEntry("body", "Lần 1")
                .containsEntry("authorUsername", "admin.ghichu");
        List<Map<String, Object>> items = (List<Map<String, Object>>) listed.getBody().get("items");
        assertThat(items)
                .extracting(item -> item.get("body"), item -> item.get("authorUsername"))
                .containsExactly(tuple("Lần 2", "admin.ghichu"), tuple("Lần 1", "admin.ghichu"));
        assertThat(items.get(0)).containsEntry("id", second.getBody().get("id"));
        ResponseEntity<Map> byLocation =
                get(adminToken, first.getHeaders().getLocation().getPath());
        assertThat(byLocation.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(byLocation.getBody())
                .containsEntry("id", first.getBody().get("id"))
                .containsEntry("body", "Lần 1")
                .containsEntry("authorUsername", "admin.ghichu");
        saveUser("ghichu.khac");
        ResponseEntity<Map> wrongOwner =
                get(adminToken, "/api/admin/users/ghichu.khac/notes/" + first.getBody().get("id"));
        assertThat(wrongOwner.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(wrongOwner.getBody()).containsEntry("code", "NOTE_NOT_FOUND");

        target.markDeleted(Instant.now());
        userRepository.save(target);
        ResponseEntity<Map> listedAfterDelete =
                get(adminToken, "/api/admin/users/ghichu.mot/notes");
        ResponseEntity<Map> addAfterDelete =
                post(adminToken, "/api/admin/users/ghichu.mot/notes", "Lần 3");

        assertThat(listedAfterDelete.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) listedAfterDelete.getBody().get("items")).hasSize(2);
        assertThat(addAfterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(addAfterDelete.getBody()).containsEntry("code", "USER_NOT_FOUND");
    }

    @Test
    @DisplayName("Ghi chú quá 200: chỉ trả 200 ghi chú mới nhất")
    void notes_moreThanLimit_returnsLatestTwoHundred() throws Exception {
        String adminToken = signInAsAdmin("admin.nhieu", "admin.nhieu@gmail.com");
        User admin = userRepository.findByUsername("admin.nhieu").orElseThrow();
        User target = saveUser("ghichu.nhieu");
        List<AdminUserNote> notes =
                IntStream.rangeClosed(1, 201)
                        .mapToObj(index -> AdminUserNote.of(target, admin.getId(), "Số " + index))
                        .toList();
        for (AdminUserNote note : notes) {
            adminUserNoteRepository.save(note);
        }

        ResponseEntity<Map> listed = get(adminToken, "/api/admin/users/ghichu.nhieu/notes");

        List<Map<String, Object>> items = (List<Map<String, Object>>) listed.getBody().get("items");
        assertThat(items).hasSize(200);
        assertThat(items.get(0)).containsEntry("body", "Số 201");
        assertThat(items).extracting(item -> item.get("body")).doesNotContain("Số 1");
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@gmail.com");
        user.setRole(Role.LEARNER);
        user.setSignupProvider(SignupProvider.EMAIL);
        user.setEmailVerifiedAt(Instant.now());
        return userRepository.save(user);
    }

    private ResponseEntity<Map> post(String adminToken, String path, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return restTemplate.postForEntity(
                path, new HttpEntity<>(Map.of("body", body), headers), Map.class);
    }

    private ResponseEntity<Map> get(String adminToken, String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
    }

    private String signInAsAdmin(String username, String email) throws Exception {
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
