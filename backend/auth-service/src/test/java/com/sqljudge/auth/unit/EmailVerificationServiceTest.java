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
import com.sqljudge.auth.event.VerificationEmailRequestedEvent;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.service.EmailVerificationService;
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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T05:00:00Z");
    private static final String EMAIL = "minh.anh@gmail.com";
    private static final String TOKEN = "link-token";
    private static final String CODE = "482913";

    @Mock private EmailTokenRepository emailTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private EmailVerificationService service;
    private User user;

    @BeforeEach
    void setUp() {
        service =
                new EmailVerificationService(
                        emailTokenRepository,
                        userRepository,
                        eventPublisher,
                        Clock.fixed(NOW, ZoneOffset.UTC));
        user = new User();
        user.setUsername("minh.anh");
        user.setEmail(EMAIL);
    }

    @Test
    @DisplayName("Phát mã: chỉ lưu hash, hết hạn sau 15 phút, mail nhận mã 6 số và link")
    void sendVerificationEmail_storesHashesAndPublishesRawSecrets() {
        when(emailTokenRepository.save(any(EmailToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.sendVerificationEmail(user);

        EmailToken token = savedToken();
        VerificationEmailRequestedEvent published = publishedEvent();
        assertThat(token.getUser()).isSameAs(user);
        assertThat(token.getPurpose()).isEqualTo(EmailTokenPurpose.VERIFY);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
        assertThat(token.getFailedAttempts()).isZero();
        assertThat(published.email()).isEqualTo(EMAIL);
        assertThat(published.code()).matches("\\d{6}");
        assertThat(published.token()).matches("[A-Za-z0-9_-]{43}");
        assertThat(token.getTokenHash()).isEqualTo(SecureTokens.sha256(published.token()));
        assertThat(token.getCodeHash()).isEqualTo(SecureTokens.sha256(published.code()));
    }

    @Test
    @DisplayName("Link còn hạn: xác thực email và đánh dấu token đã dùng")
    void confirmByToken_activeToken_verifiesUser() {
        EmailToken token = activeToken();
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256(TOKEN)))
                .thenReturn(Optional.of(token));

        service.confirmByToken(TOKEN);

        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
        assertThat(token.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Link hết hạn, đã dùng hoặc không tồn tại đều trả 422 TOKEN_INVALID_OR_EXPIRED")
    void confirmByToken_inactiveOrUnknown_throwsInvalid() {
        EmailToken expired = activeToken();
        expired.setExpiresAt(NOW);
        EmailToken used = activeToken();
        used.setUsedAt(NOW.minusSeconds(1));
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256("expired")))
                .thenReturn(Optional.of(expired));
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256("used")))
                .thenReturn(Optional.of(used));
        when(emailTokenRepository.findByTokenHash(SecureTokens.sha256("unknown")))
                .thenReturn(Optional.empty());

        assertInvalid(() -> service.confirmByToken("expired"));
        assertInvalid(() -> service.confirmByToken("used"));
        assertInvalid(() -> service.confirmByToken("unknown"));
        assertThat(user.getEmailVerifiedAt()).isNull();
    }

    @Test
    @DisplayName("Mã đúng (email khác hoa thường) thì xác thực email")
    void confirmByCode_correctCode_verifiesUser() {
        EmailToken token = activeToken();
        stubLatestActiveToken(token);

        service.confirmByCode(" Minh.Anh@Gmail.com ", CODE);

        assertThat(user.getEmailVerifiedAt()).isEqualTo(NOW);
        assertThat(token.getUsedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Mã sai thì tăng số lần sai và trả 422")
    void confirmByCode_wrongCode_countsFailedAttempt() {
        EmailToken token = activeToken();
        stubLatestActiveToken(token);

        assertInvalid(() -> service.confirmByCode(EMAIL, "000000"));

        assertThat(token.getFailedAttempts()).isEqualTo(1);
        assertThat(token.isActiveAt(NOW)).isTrue();
        assertThat(user.getEmailVerifiedAt()).isNull();
    }

    @Test
    @DisplayName("Sai lần thứ 5 thì mã bị huỷ")
    void confirmByCode_fifthWrongAttempt_invalidatesToken() {
        EmailToken token = activeToken();
        token.setFailedAttempts(4);
        stubLatestActiveToken(token);

        assertInvalid(() -> service.confirmByCode(EMAIL, "000000"));

        assertThat(token.getFailedAttempts()).isEqualTo(5);
        assertThat(token.isActiveAt(NOW)).isFalse();
    }

    @Test
    @DisplayName(
            "Đã sai đủ 20 lần trong 24 giờ: khoá nhập mã kể cả mã đúng, không tính thêm lần sai")
    void confirmByCode_dailyFailureCapReached_rejectsEvenCorrectCode() {
        EmailToken token = activeToken();
        stubLatestActiveToken(token);
        when(emailTokenRepository.sumFailedAttemptsSince(
                        user, EmailTokenPurpose.VERIFY, NOW.minus(Duration.ofHours(24))))
                .thenReturn(20L);

        assertThatThrownBy(() -> service.confirmByCode(EMAIL, CODE))
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.VERIFICATION_CODE_LOCKED);
        assertThat(token.getFailedAttempts()).isZero();
        assertThat(user.getEmailVerifiedAt()).isNull();
    }

    @Test
    @DisplayName("Không có mã nào còn hiệu lực cho email đó thì trả 422, không lộ email có tồn tại")
    void confirmByCode_noActiveToken_throwsInvalid() {
        when(emailTokenRepository.findFirstByUserEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                        "ai.do@gmail.com", EmailTokenPurpose.VERIFY))
                .thenReturn(Optional.empty());

        assertInvalid(() -> service.confirmByCode("ai.do@gmail.com", CODE));
    }

    @Test
    @DisplayName("Gửi lại sau hơn 60 giây: huỷ mã cũ và gửi mã mới")
    void resendVerificationEmail_afterCooldown_issuesNewToken() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(emailTokenRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(
                        user, EmailTokenPurpose.VERIFY))
                .thenReturn(Optional.of(tokenCreatedAt(NOW.minusSeconds(61))));
        when(emailTokenRepository.save(any(EmailToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.resendVerificationEmail("MINH.ANH@gmail.com");

        verify(emailTokenRepository).invalidateUnusedTokens(user, EmailTokenPurpose.VERIFY, NOW);
        assertThat(savedToken().getPurpose()).isEqualTo(EmailTokenPurpose.VERIFY);
        assertThat(publishedEvent().email()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("Gửi lại trong vòng 60 giây thì bỏ qua")
    void resendVerificationEmail_withinCooldown_doesNothing() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));
        when(emailTokenRepository.findFirstByUserAndPurposeOrderByCreatedAtDesc(
                        user, EmailTokenPurpose.VERIFY))
                .thenReturn(Optional.of(tokenCreatedAt(NOW.minusSeconds(30))));

        service.resendVerificationEmail(EMAIL);

        verify(emailTokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Email đã xác thực, đã xoá hoặc không tồn tại thì bỏ qua mà không báo lỗi")
    void resendVerificationEmail_ineligibleUser_doesNothing() {
        User verified = new User();
        verified.setEmailVerifiedAt(NOW.minusSeconds(3600));
        User deleted = new User();
        deleted.setDeletedAt(NOW.minusSeconds(3600));
        when(userRepository.findByEmail("verified@gmail.com")).thenReturn(Optional.of(verified));
        when(userRepository.findByEmail("deleted@gmail.com")).thenReturn(Optional.of(deleted));
        when(userRepository.findByEmail("unknown@gmail.com")).thenReturn(Optional.empty());

        service.resendVerificationEmail("verified@gmail.com");
        service.resendVerificationEmail("deleted@gmail.com");
        service.resendVerificationEmail("unknown@gmail.com");

        verifyNoInteractions(emailTokenRepository, eventPublisher);
    }

    private EmailToken activeToken() {
        EmailToken token = new EmailToken();
        token.setUser(user);
        token.setPurpose(EmailTokenPurpose.VERIFY);
        token.setTokenHash(SecureTokens.sha256(TOKEN));
        token.setCodeHash(SecureTokens.sha256(CODE));
        token.setExpiresAt(NOW.plus(Duration.ofMinutes(10)));
        return token;
    }

    private EmailToken tokenCreatedAt(Instant createdAt) {
        EmailToken token = activeToken();
        ReflectionTestUtils.setField(token, "createdAt", createdAt);
        return token;
    }

    private void stubLatestActiveToken(EmailToken token) {
        when(emailTokenRepository.findFirstByUserEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                        EMAIL, EmailTokenPurpose.VERIFY))
                .thenReturn(Optional.of(token));
    }

    private EmailToken savedToken() {
        ArgumentCaptor<EmailToken> captor = ArgumentCaptor.forClass(EmailToken.class);
        verify(emailTokenRepository).save(captor.capture());
        return captor.getValue();
    }

    private VerificationEmailRequestedEvent publishedEvent() {
        ArgumentCaptor<VerificationEmailRequestedEvent> captor =
                ArgumentCaptor.forClass(VerificationEmailRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    private static void assertInvalid(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(BusinessRuleException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.TOKEN_INVALID_OR_EXPIRED);
    }
}
