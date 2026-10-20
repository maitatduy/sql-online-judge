package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.AdminActionRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AdminUserAvatarIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private AdminActionRepository adminActionRepository;
    @Autowired private InMemoryAvatarStorage avatarStorage;

    @Test
    @DisplayName(
            "Admin đổi avatar: 200, chi tiết người dùng có URL mới, ảnh cũ bị xoá khỏi kho, có"
                    + " nhật ký")
    void replaceAvatar_endToEnd() throws Exception {
        String adminToken = signInAsAdmin("admin.anh", "admin.anh@gmail.com");
        registerVerified("co.anh", "co.anh@gmail.com");

        ResponseEntity<Map> first = upload("co.anh", PNG, adminToken);
        String firstPublicId =
                userRepository.findByUsername("co.anh").orElseThrow().getAvatarPublicId();
        ResponseEntity<Map> second = upload("co.anh", PNG, adminToken);
        ResponseEntity<Map> detail = get("/api/admin/users/co.anh", adminToken);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(detail.getBody()).containsEntry("avatarUrl", second.getBody().get("avatarUrl"));
        assertThat(avatarStorage.contains(firstPublicId)).isFalse();
        User user = userRepository.findByUsername("co.anh").orElseThrow();
        assertThat(avatarStorage.contains(user.getAvatarPublicId())).isTrue();
        assertThat(adminActionRepository.findAll())
                .filteredOn(action -> user.getId().equals(action.getTargetUserId()))
                .extracting(action -> action.getAction())
                .containsExactly(AdminActionType.UPDATE_AVATAR, AdminActionType.UPDATE_AVATAR);
    }

    @Test
    @DisplayName("File giả ảnh hoặc vượt giới hạn tải lên: 400, không đổi avatar")
    void replaceAvatar_invalidFiles_rejected() throws Exception {
        String adminToken = signInAsAdmin("admin.anh2", "admin.anh2@gmail.com");
        registerVerified("anh.sai", "anh.sai@gmail.com");
        byte[] html = "<html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);
        byte[] tooLarge = Arrays.copyOf(PNG, 4 * 1024 * 1024);

        ResponseEntity<Map> fake = upload("anh.sai", html, adminToken);
        ResponseEntity<Map> huge = upload("anh.sai", tooLarge, adminToken);

        assertThat(fake.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(fake.getBody()).containsEntry("code", "VALIDATION_FAILED");
        assertThat(huge.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(userRepository.findByUsername("anh.sai").orElseThrow().getAvatarUrl()).isNull();
    }

    private ResponseEntity<Map> upload(String username, byte[] bytes, String adminToken) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add(
                "file",
                new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                        return "anh.png";
                    }
                });
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return restTemplate.exchange(
                "/api/admin/users/" + username + "/avatar",
                HttpMethod.PUT,
                new HttpEntity<>(form, headers),
                Map.class);
    }

    private ResponseEntity<Map> get(String path, String adminToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(adminToken);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
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
        return (String)
                restTemplate
                        .postForEntity(
                                "/api/auth/login",
                                Map.of("email", email, "password", PASSWORD, "rememberMe", false),
                                Map.class)
                        .getBody()
                        .get("accessToken");
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
}
