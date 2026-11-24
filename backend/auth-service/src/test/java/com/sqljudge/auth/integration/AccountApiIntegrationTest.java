package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.SignupProvider;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.security.Role;
import java.time.Instant;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AccountApiIntegrationTest extends ContainerSupport {

    private static final String PASSWORD = "matkhau-manh-2026";
    private static final String NEW_PASSWORD = "matkhau-moi-2026";
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0};

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private InMemoryAvatarStorage avatarStorage;

    @Test
    @DisplayName("Sửa hồ sơ rồi hồ sơ công khai hiện đúng thông tin mới; khách cũng xem được")
    void updateProfile_showsOnPublicProfile() {
        String token = signIn("ho.so", "ho.so@gmail.com");

        ResponseEntity<Map> updated =
                send(
                        HttpMethod.PATCH,
                        "/api/account/profile",
                        Map.of(
                                "displayName", "  Hồ Sơ  ",
                                "bio", "Thích window function",
                                "school", "ĐH Bách khoa",
                                "location", " ",
                                "websiteUrl", "https://hoso.dev"),
                        token);
        ResponseEntity<Map> publicProfile =
                restTemplate.getForEntity("/api/users/ho.so", Map.class);

        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).containsEntry("displayName", "Hồ Sơ");
        assertThat(publicProfile.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publicProfile.getBody())
                .containsEntry("username", "ho.so")
                .containsEntry("bio", "Thích window function")
                .containsEntry("school", "ĐH Bách khoa")
                .containsEntry("location", null)
                .containsEntry("websiteUrl", "https://hoso.dev")
                .doesNotContainKey("email");
        assertThat(restTemplate.getForEntity("/api/users/khong.co", Map.class).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Tên hiển thị trống hoặc website không phải http(s): 400 kèm lỗi từng trường")
    void updateProfile_validates() {
        String token = signIn("ho.so.sai", "ho.so.sai@gmail.com");

        ResponseEntity<Map> invalid =
                send(
                        HttpMethod.PATCH,
                        "/api/account/profile",
                        Map.of("displayName", " ", "websiteUrl", "javascript:alert(1)"),
                        token);

        assertThat(invalid.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(invalid.getBody().get("errors").toString())
                .contains("displayName")
                .contains("websiteUrl");
    }

    @Test
    @DisplayName("Đổi mật khẩu: sai mật khẩu cũ thì 400, đúng thì đăng nhập được bằng mật khẩu mới")
    void changePassword_requiresCurrentPassword() {
        String token = signIn("doi.mk", "doi.mk@gmail.com");

        ResponseEntity<Map> wrong =
                send(
                        HttpMethod.POST,
                        "/api/account/password",
                        Map.of("currentPassword", "sai-mat-khau", "newPassword", NEW_PASSWORD),
                        token);
        ResponseEntity<Map> changed =
                send(
                        HttpMethod.POST,
                        "/api/account/password",
                        Map.of("currentPassword", PASSWORD, "newPassword", NEW_PASSWORD),
                        token);

        assertThat(wrong.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(wrong.getBody().get("errors").toString()).contains("currentPassword");
        assertThat(changed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(login("doi.mk@gmail.com", NEW_PASSWORD).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Tự đổi rồi gỡ avatar; ảnh cũ bị xoá khỏi kho; khách bị 401")
    void avatar_replaceAndRemove() {
        String token = signIn("anh.dai.dien", "anh.dai.dien@gmail.com");

        ResponseEntity<Map> uploaded = upload(token);
        String publicId =
                userRepository.findByUsername("anh.dai.dien").orElseThrow().getAvatarPublicId();
        ResponseEntity<Map> removed = send(HttpMethod.DELETE, "/api/account/avatar", null, token);

        assertThat(uploaded.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(uploaded.getBody().get("avatarUrl")).isNotNull();
        assertThat(removed.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(avatarStorage.contains(publicId)).isFalse();
        assertThat(send(HttpMethod.GET, "/api/account/profile", null, null).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private String signIn(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setDisplayName(username);
        user.setEmail(email);
        user.setRole(Role.LEARNER);
        user.setSignupProvider(SignupProvider.EMAIL);
        user.setEmailVerifiedAt(Instant.now());
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        userRepository.save(user);
        return (String) login(email, PASSWORD).getBody().get("accessToken");
    }

    private ResponseEntity<Map> login(String email, String password) {
        return restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("email", email, "password", password, "rememberMe", false),
                Map.class);
    }

    private ResponseEntity<Map> upload(String token) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add(
                "file",
                new ByteArrayResource(PNG) {
                    @Override
                    public String getFilename() {
                        return "anh.png";
                    }
                });
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        return restTemplate.exchange(
                "/api/account/avatar", HttpMethod.PUT, new HttpEntity<>(form, headers), Map.class);
    }

    private ResponseEntity<Map> send(HttpMethod method, String path, Object body, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(body, headers), Map.class);
    }
}
