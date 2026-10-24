package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.EmailToken;
import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.integration.MailpitClient.ReceivedMail;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.SecureTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EmailVerificationIntegrationTest extends ContainerSupport {

    private static final String CONFIRM_PATH = "/api/auth/email-verification/confirm";

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailTokenRepository emailTokenRepository;

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
    @DisplayName(
            "Đã sai 20 lần trong 24 giờ qua nhiều mã: khoá nhập mã kể cả mã đúng, link trong mail"
                    + " vẫn dùng được")
    void confirmByCode_dailyFailureCapReached_onlyLinkWorks() throws Exception {
        String email = "doan.nhieu@gmail.com";
        ReceivedMail mail = registerAndReadMail("doan.nhieu", email);
        User user = userRepository.findByEmail(email).orElseThrow();
        for (int previousCode = 0; previousCode < 4; previousCode++) {
            emailTokenRepository.save(exhaustedToken(user));
        }

        ResponseEntity<Map> correctCode =
                confirm(
                        Map.of(
                                "email",
                                email,
                                "code",
                                MailpitContainer.firstGroup(MailpitContainer.CODE, mail.text())));
        ResponseEntity<Map> link =
                confirm(
                        Map.of(
                                "token",
                                MailpitContainer.firstGroup(MailpitContainer.LINK, mail.text())));

        assertThat(correctCode.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(correctCode.getBody()).containsEntry("code", "VERIFICATION_CODE_LOCKED");
        assertThat(link.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(isVerified("doan.nhieu")).isTrue();
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

    private static EmailToken exhaustedToken(User user) {
        EmailToken token = new EmailToken();
        token.setUser(user);
        token.setPurpose(EmailTokenPurpose.VERIFY);
        token.setTokenHash(SecureTokens.sha256(SecureTokens.newToken()));
        token.setCodeHash(SecureTokens.sha256(SecureTokens.newCode()));
        token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
        token.setFailedAttempts(5);
        token.setUsedAt(Instant.now());
        return token;
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
