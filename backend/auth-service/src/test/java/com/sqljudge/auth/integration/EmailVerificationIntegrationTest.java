package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.integration.MailpitClient.ReceivedMail;
import com.sqljudge.auth.repository.UserRepository;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EmailVerificationIntegrationTest extends MySqlContainerSupport {

    private static final String CONFIRM_PATH = "/api/auth/email-verification/confirm";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        MailpitContainer.registerMailProperties(registry);
    }

    @Test
    @DisplayName("Nhập đúng mã 6 số thì email được xác thực, dùng lại mã thì 422")
    void confirmByCode_correctCode_verifiesOnce() throws Exception {
        String email = "xac.thuc.ma@gmail.com";
        String code =
                MailpitContainer.firstGroup(
                        MailpitContainer.CODE, registerAndReadMail("xac.thuc.ma", email).text());

        ResponseEntity<Map> first = confirm(Map.of("email", email, "code", code));
        ResponseEntity<Map> reused = confirm(Map.of("email", email, "code", code));

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isVerified("xac.thuc.ma")).isTrue();
        assertThat(reused.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(reused.getBody()).containsEntry("code", "TOKEN_INVALID_OR_EXPIRED");
    }

    @Test
    @DisplayName("Bấm link trong email thì email được xác thực")
    void confirmByToken_linkFromMail_verifies() throws Exception {
        String token =
                MailpitContainer.firstGroup(
                        MailpitContainer.LINK,
                        registerAndReadMail("xac.thuc.link", "xac.thuc.link@gmail.com").text());

        ResponseEntity<Map> response = confirm(Map.of("token", token));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isVerified("xac.thuc.link")).isTrue();
    }

    @Test
    @DisplayName("Sai mã 5 lần thì mã bị huỷ: lần lỗi được lưu dù request trả 422")
    void confirmByCode_fiveWrongAttempts_invalidatesCode() throws Exception {
        String email = "do.ma@gmail.com";
        String code =
                MailpitContainer.firstGroup(
                        MailpitContainer.CODE, registerAndReadMail("do.ma", email).text());
        String wrongCode = code.equals("000000") ? "111111" : "000000";

        for (int attempt = 0; attempt < 5; attempt++) {
            assertThat(confirm(Map.of("email", email, "code", wrongCode)).getStatusCode())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        ResponseEntity<Map> correctAfterLockout = confirm(Map.of("email", email, "code", code));

        assertThat(correctAfterLockout.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(isVerified("do.ma")).isFalse();
    }

    @Test
    @DisplayName("Gửi lại mã luôn trả 202, kể cả với email không tồn tại")
    void resend_unknownEmail_returnsAcceptedWithoutLeaking() {
        ResponseEntity<Void> response =
                restTemplate.postForEntity(
                        "/api/auth/email-verification",
                        Map.of("email", "khong.ton.tai@gmail.com"),
                        Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
    }

    private ReceivedMail registerAndReadMail(String username, String email) throws Exception {
        Map<String, Object> body =
                Map.of(
                        "username",
                        username,
                        "email",
                        email,
                        "password",
                        "matkhau-manh-2026",
                        "acceptedTerms",
                        true);
        restTemplate.postForEntity("/api/auth/register", body, Map.class);
        return MailpitContainer.client().awaitMailTo(email);
    }

    private ResponseEntity<Map> confirm(Map<String, String> body) {
        return restTemplate.postForEntity(CONFIRM_PATH, body, Map.class);
    }

    private boolean isVerified(String username) {
        return userRepository.findAll().stream()
                        .filter(user -> user.getUsername().equals(username))
                        .findFirst()
                        .orElseThrow()
                        .getEmailVerifiedAt()
                != null;
    }
}
