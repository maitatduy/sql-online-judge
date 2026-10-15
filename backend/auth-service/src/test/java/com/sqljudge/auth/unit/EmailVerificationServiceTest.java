package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.entity.EmailToken;
import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.VerificationEmailRequestedEvent;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.service.EmailVerificationService;
import com.sqljudge.auth.service.SecureTokens;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T05:00:00Z");

    @Mock private EmailTokenRepository emailTokenRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService =
                new EmailVerificationService(
                        emailTokenRepository, eventPublisher, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Phát mã: chỉ lưu hash, hết hạn sau 15 phút, mail nhận mã 6 số và link")
    void sendVerificationEmail_storesHashesAndPublishesRawSecrets() {
        User user = new User();
        user.setUsername("minh.anh");
        user.setEmail("minh.anh@gmail.com");
        when(emailTokenRepository.save(any(EmailToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.sendVerificationEmail(user);

        ArgumentCaptor<EmailToken> savedToken = ArgumentCaptor.forClass(EmailToken.class);
        verify(emailTokenRepository).save(savedToken.capture());
        ArgumentCaptor<VerificationEmailRequestedEvent> event =
                ArgumentCaptor.forClass(VerificationEmailRequestedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());

        EmailToken token = savedToken.getValue();
        VerificationEmailRequestedEvent published = event.getValue();
        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getPurpose()).isEqualTo(EmailTokenPurpose.VERIFY);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(token.getFailedAttempts()).isZero();
        assertThat(token.getUsedAt()).isNull();
        assertThat(published.email()).isEqualTo("minh.anh@gmail.com");
        assertThat(published.username()).isEqualTo("minh.anh");
        assertThat(published.code()).matches("\\d{6}");
        assertThat(published.token()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(token.getTokenHash()).isEqualTo(SecureTokens.sha256(published.token()));
        assertThat(token.getCodeHash()).isEqualTo(SecureTokens.sha256(published.code()));
        assertThat(token.getTokenHash()).doesNotContain(published.token());
    }

    @Test
    @DisplayName("Mỗi lần phát là một token mới")
    void newToken_isUniqueAndUrlSafe() {
        String first = SecureTokens.newToken();
        String second = SecureTokens.newToken();

        assertThat(first).isNotEqualTo(second).matches("[A-Za-z0-9_-]{43}");
    }
}
