package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.EmailToken;
import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.VerificationEmailRequestedEvent;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ConflictException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    static final Duration VERIFICATION_TTL = Duration.ofMinutes(15);
    static final int MAX_FAILED_ATTEMPTS = 5;
    static final int MAX_DAILY_FAILED_ATTEMPTS = 20;
    static final Duration FAILURE_CAP_WINDOW = Duration.ofHours(24);

    private static final EmailTokenPurpose PURPOSE = EmailTokenPurpose.VERIFY;

    private final EmailTokenRepository emailTokenRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public void sendVerificationEmail(User user) {
        String token = SecureTokens.newToken();
        String code = SecureTokens.newCode();
        EmailToken emailToken = new EmailToken();
        emailToken.setUser(user);
        emailToken.setPurpose(PURPOSE);
        emailToken.setTokenHash(SecureTokens.sha256(token));
        emailToken.setCodeHash(SecureTokens.sha256(code));
        emailToken.setExpiresAt(clock.instant().plus(VERIFICATION_TTL));
        emailTokenRepository.save(emailToken);
        eventPublisher.publishEvent(
                new VerificationEmailRequestedEvent(
                        user.getId(), user.getEmail(), user.getUsername(), token, code));
    }

    @Transactional
    public UUID confirmByToken(String token) {
        Instant now = clock.instant();
        EmailToken emailToken =
                emailTokenRepository
                        .findByTokenHash(SecureTokens.sha256(token))
                        .filter(found -> found.getPurpose() == PURPOSE && found.isActiveAt(now))
                        .orElseThrow(EmailVerificationService::invalidToken);
        return markVerified(emailToken, now);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public UUID confirmByCode(String email, String code) {
        Instant now = clock.instant();
        EmailToken emailToken =
                emailTokenRepository
                        .findFirstByUserEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                                EmailAddresses.normalize(email), PURPOSE)
                        .filter(found -> found.isActiveAt(now))
                        .orElseThrow(EmailVerificationService::invalidToken);
        if (hasReachedDailyFailureCap(emailToken.getUser(), now)) {
            throw new BusinessRuleException(
                    ErrorCode.VERIFICATION_CODE_LOCKED,
                    "Bạn đã nhập sai mã quá nhiều lần. Hãy bấm liên kết trong email để xác"
                            + " thực.");
        }
        if (!SecureTokens.matches(code, emailToken.getCodeHash())) {
            emailToken.recordFailedAttempt(now, MAX_FAILED_ATTEMPTS);
            throw invalidToken();
        }
        return markVerified(emailToken, now);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        Instant now = clock.instant();
        userRepository
                .findByEmail(EmailAddresses.normalize(email))
                .filter(User::isAwaitingEmailVerification)
                .filter(
                        user ->
                                ResendCooldown.remaining(emailTokenRepository, user, PURPOSE, now)
                                        .isZero())
                .ifPresent(user -> reissue(user, now));
    }

    @Transactional
    public void resendVerificationEmailTo(User user) {
        if (user.isEmailVerified()) {
            throw new ConflictException(ErrorCode.EMAIL_ALREADY_VERIFIED, "Email đã được xác minh");
        }
        Instant now = clock.instant();
        ResendCooldown.requirePassed(emailTokenRepository, user, PURPOSE, now);
        reissue(user, now);
    }

    private void reissue(User user, Instant now) {
        emailTokenRepository.invalidateUnusedTokens(user, PURPOSE, now);
        sendVerificationEmail(user);
    }

    private boolean hasReachedDailyFailureCap(User user, Instant now) {
        long failures =
                emailTokenRepository.sumFailedAttemptsSince(
                        user, PURPOSE, now.minus(FAILURE_CAP_WINDOW));
        return failures >= MAX_DAILY_FAILED_ATTEMPTS;
    }

    private static UUID markVerified(EmailToken emailToken, Instant now) {
        emailToken.markUsed(now);
        emailToken.getUser().markEmailVerified(now);
        return emailToken.getUser().getId();
    }

    private static BusinessRuleException invalidToken() {
        return new BusinessRuleException(
                ErrorCode.TOKEN_INVALID_OR_EXPIRED, "Mã không hợp lệ hoặc đã hết hạn");
    }
}
