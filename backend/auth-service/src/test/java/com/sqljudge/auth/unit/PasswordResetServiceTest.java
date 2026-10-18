package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.entity.EmailToken;
import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.PasswordResetEmailRequestedEvent;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.auth.service.PasswordResetService;
import com.sqljudge.auth.service.SecureTokens;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T05:00:00Z");
    private static final String EMAIL = "minh.anh@gmail.com";
    private static final String TOKEN = "reset-link-token";

    @Mock private UserRepository userRepository;
    @Mock private EmailTokenRepository emailTokenRepository;
    @Mock private UserSessionRepository userSessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PasswordResetService service;
    private User user;

    @BeforeEach
    void setUp() {
        service =
                new PasswordResetService(
                        userRepository,
                        emailTokenRepository,
                        userSessionRepository,
                        passwordEncoder,
                        eventPublisher,
                        Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User();
        user.setUsername("minh.anh");
        user.setEmail(EMAIL);
        user.setPasswordHash("old-hash");
    }

    @Test
    @DisplayName("Yêu cầu đặt lại: huỷ link cũ, lưu hash link mới sống 30 phút, gửi mail")
    void requestReset_existingUser_issuesThirtyMinuteLink() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(emailTokenRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(
                        user, EmailTokenPurpose.RESET))
                .thenReturn(Optional.empty());
        when(emailTokenRepository.save(any(EmailToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.requestReset(" Minh.Anh@Gmail.com ");

        verify(emailTokenRepository).invalidateUnusedTokens(user, EmailTokenPurpose.RESET, NOW);
        ArgumentCaptor<EmailToken> saved = ArgumentCaptor.forClass(EmailToken.class);
        verify(emailTokenRepository).save(saved.capture());
        ArgumentCaptor<PasswordResetEmailRequestedEvent> event =
                ArgumentCaptor.forClass(PasswordResetEmailRequestedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(saved.getValue().getPurpose()).isEqualTo(EmailTokenPurpose.RESET);
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        assertThat(saved.getValue().getCodeHash()).isNull();
        assertThat(saved.getValue().getTokenHash())
                .isEqualTo(SecureTokens.sha256(event.getValue().token()));
        assertThat(event.getValue().email()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Yêu cầu lại trong 60 giây thì bỏ qua")
    void requestReset_withinCooldown_doesNothing() {
        EmailToken recent = new EmailToken();
        ReflectionTestUtils.setField(recent, "createdAt", NOW.minusSeconds(20));
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(emailTokenRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(
                        user, EmailTokenPurpose.RESET))
                .thenReturn(Optional.of(recent));

        service.requestReset(EMAIL);

        verify(emailTokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Email không tồn tại hoặc tài khoản đã xoá: im lặng, không lộ thông tin")
    void requestReset_unknownOrDeleted_doesNothing() {
        User deleted = new User();
        deleted.setDeletedAt(NOW.minusSeconds(60));
        when(userRepository.findByEmail("ai.do@gmail.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("da.xoa@gmail.com")).thenReturn(Optional.of(deleted));

        service.requestReset("ai.do@gmail.com");
        service.requestReset("da.xoa@gmail.com");

        verifyNoInteractions(emailTokenRepository, eventPublisher);
    }

    @Test
    @DisplayName("Link còn hạn: đổi mật khẩu, đánh dấu link đã dùng, đăng xuất mọi phiên")
    void resetPassword_activeLink_changesPasswordAndRevokesSessions() {
        EmailToken token = resetToken(NOW.plus(Duration.ofMinutes(10)));
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256(TOKEN)))
                .thenReturn(Optional.of(token));
        when(passwordEncoder.encode("matkhau-moi-2026")).thenReturn("new-hash");

        service.resetPassword(TOKEN, "matkhau-moi-2026");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(token.getUsedAt()).isEqualTo(NOW);
        verify(userSessionRepository).revokeAllActiveSessions(user, NOW);
    }

    @Test
    @DisplayName("Link mời do admin gửi: đặt mật khẩu lần đầu và xác minh email")
    void resetPassword_inviteLink_setsPasswordAndVerifiesEmail() {
        user.setPasswordHash(null);
        EmailToken invite = resetToken(NOW.plus(Duration.ofDays(6)));
        invite.setPurpose(EmailTokenPurpose.INVITE);
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256(TOKEN)))
                .thenReturn(Optional.of(invite));
        when(passwordEncoder.encode("matkhau-moi-2026")).thenReturn("new-hash");

        service.resetPassword(TOKEN, "matkhau-moi-2026");

        assertThat(user.getPasswordHash()).isEqualTo("new-hash");
        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
        assertThat(invite.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Dùng link reset: link mời và link reset khác còn chờ đều bị vô hiệu")
    void resetPassword_resetLink_invalidatesOtherPasswordLinks() {
        EmailToken token = resetToken(NOW.plus(Duration.ofMinutes(10)));
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256(TOKEN)))
                .thenReturn(Optional.of(token));

        service.resetPassword(TOKEN, "matkhau-moi-2026");

        verify(emailTokenRepository).invalidateUnusedTokens(user, EmailTokenPurpose.RESET, NOW);
        verify(emailTokenRepository).invalidateUnusedTokens(user, EmailTokenPurpose.INVITE, NOW);
    }

    @Test
    @DisplayName("Dùng link mời: link reset còn chờ cũng bị vô hiệu")
    void resetPassword_inviteLink_invalidatesPendingResetLinks() {
        EmailToken invite = resetToken(NOW.plus(Duration.ofDays(6)));
        invite.setPurpose(EmailTokenPurpose.INVITE);
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256(TOKEN)))
                .thenReturn(Optional.of(invite));

        service.resetPassword(TOKEN, "matkhau-moi-2026");

        verify(emailTokenRepository).invalidateUnusedTokens(user, EmailTokenPurpose.RESET, NOW);
        verify(emailTokenRepository).invalidateUnusedTokens(user, EmailTokenPurpose.INVITE, NOW);
    }

    @Test
    @DisplayName("Link đặt lại mật khẩu cũng chứng minh sở hữu email nên xác minh email")
    void resetPassword_resetLink_verifiesEmail() {
        EmailToken token = resetToken(NOW.plus(Duration.ofMinutes(10)));
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256(TOKEN)))
                .thenReturn(Optional.of(token));

        service.resetPassword(TOKEN, "matkhau-moi-2026");

        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Email đã xác minh từ trước thì giữ nguyên thời điểm xác minh cũ")
    void resetPassword_alreadyVerified_keepsOriginalVerificationTime() {
        Instant verifiedAt = NOW.minus(Duration.ofDays(30));
        user.setEmailVerifiedAt(verifiedAt);
        EmailToken token = resetToken(NOW.plus(Duration.ofMinutes(10)));
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256(TOKEN)))
                .thenReturn(Optional.of(token));

        service.resetPassword(TOKEN, "matkhau-moi-2026");

        assertThat(user.getEmailVerifiedAt()).isEqualTo(verifiedAt);
    }

    @Test
    @DisplayName("Link hết hạn, đã dùng, sai mục đích hoặc không tồn tại: 422, mật khẩu giữ nguyên")
    void resetPassword_invalidLink_rejects() {
        EmailToken expired = resetToken(NOW);
        EmailToken verifyToken = resetToken(NOW.plus(Duration.ofMinutes(10)));
        verifyToken.setPurpose(EmailTokenPurpose.VERIFY);
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256("expired")))
                .thenReturn(Optional.of(expired));
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256("verify")))
                .thenReturn(Optional.of(verifyToken));
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256("unknown")))
                .thenReturn(Optional.empty());

        assertInvalid("expired");
        assertInvalid("verify");
        assertInvalid("unknown");
        assertThat(user.getPasswordHash()).isEqualTo("old-hash");
        verifyNoInteractions(userSessionRepository);
    }

    private EmailToken resetToken(Instant expiresAt) {
        EmailToken token = new EmailToken();
        token.setUser(user);
        token.setPurpose(EmailTokenPurpose.RESET);
        token.setTokenHash(SecureTokens.sha256(TOKEN));
        token.setExpiresAt(expiresAt);
        return token;
    }

    private void assertInvalid(String token) {
        assertThatThrownBy(() -> service.resetPassword(token, "matkhau-moi-2026"))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.TOKEN_INVALID_OR_EXPIRED);
    }
}
