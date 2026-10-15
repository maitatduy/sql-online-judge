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
class RegistrationIntegrationTest extends MySqlContainerSupport {

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
        assertThat(response.getHeaders().getLocation()).hasToString("/api/users/tich.hop");
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
    @DisplayName("Đăng ký lại cùng email khác hoa thường trả 409 EMAIL_TAKEN")
    void register_duplicateEmail_returnsConflict() {
        register("lan.mot", "trung.email@gmail.com");

        ResponseEntity<Map> response = register("lan.hai", "TRUNG.EMAIL@gmail.com");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("code", "EMAIL_TAKEN");
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
