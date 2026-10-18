package com.sqljudge.auth.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.entity.EmailToken;
import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.integration.MailpitClient.ReceivedMail;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.SecureTokens;
import java.util.List;
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
class RegistrationIntegrationTest extends ContainerSupport {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailTokenRepository emailTokenRepository;

    @DynamicPropertySource
    static void mailProperties(DynamicPropertyRegistry registry) {
        MailpitContainer.registerMailProperties(registry);
    }

    @Test
    @DisplayName(
            "Đăng ký thật: lưu user chưa xác minh, BCrypt cost 12, mail có mã và link khớp hash")
    void register_persistsUserAndDeliversVerificationMail() throws Exception {
        ResponseEntity<Map> response = register("tich.hop", "Tich.Hop@Gmail.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isNull();
        assertThat(response.getBody()).containsOnlyKeys("email");

        User user =
                userRepository.findAll().stream()
                        .filter(saved -> saved.getUsername().equals("tich.hop"))
                        .findFirst()
                        .orElseThrow();
        assertThat(user.getEmail()).isEqualTo("tich.hop@gmail.com");
        assertThat(user.getEmailVerifiedAt()).isNull();
        assertThat(user.getPasswordHash()).startsWith("$2a$12$");

        ReceivedMail mail = MailpitContainer.client().awaitMailTo("tich.hop@gmail.com");
        String code = MailpitContainer.firstGroup(MailpitContainer.CODE, mail.text());
        String token = MailpitContainer.firstGroup(MailpitContainer.LINK, mail.text());
        List<EmailToken> tokens =
                emailTokenRepository.findAll().stream()
                        .filter(saved -> saved.getUser().getId().equals(user.getId()))
                        .toList();
        assertThat(mail.subject()).isEqualTo("Xác thực email SQL Judge");
        assertThat(tokens)
                .singleElement()
                .satisfies(
                        saved -> {
                            assertThat(saved.getPurpose()).isEqualTo(EmailTokenPurpose.VERIFY);
                            assertThat(saved.getCodeHash()).isEqualTo(SecureTokens.sha256(code));
                            assertThat(saved.getTokenHash()).isEqualTo(SecureTokens.sha256(token));
                        });
    }

    @Test
    @DisplayName(
            "Đăng ký lại bằng email đã có: trả 201 giống hệt đăng ký mới để không lộ email, không"
                    + " tạo user, chủ email nhận mail báo đã có tài khoản")
    void register_existingEmail_respondsLikeNewAndNotifiesOwner() throws Exception {
        register("lan.mot", "trung.email@gmail.com");
        long usersBefore = userRepository.count();

        ResponseEntity<Map> response = register("lan.hai", "TRUNG.EMAIL@gmail.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsOnlyKeys("email");
        assertThat(response.getBody()).containsEntry("email", "trung.email@gmail.com");
        assertThat(userRepository.count()).isEqualTo(usersBefore);
        ReceivedMail mail =
                MailpitContainer.client()
                        .awaitMailWithSubject(
                                "trung.email@gmail.com", "Email này đã có tài khoản SQL Judge");
        assertThat(mail.text()).contains("lan.mot").contains("/forgot-password");
    }

    private ResponseEntity<Map> register(String username, String email) {
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
        return restTemplate.postForEntity("/api/auth/register", body, Map.class);
    }
}
