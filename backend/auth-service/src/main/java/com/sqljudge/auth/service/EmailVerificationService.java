package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.EmailToken;
import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.VerificationEmailRequestedEvent;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    static final Duration VERIFICATION_TTL = Duration.ofMinutes(15);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_FAILED_ATTEMPTS = 5;

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
    public void confirmByToken(String token) {
        Instant now = clock.instant();
        EmailToken emailToken =
                emailTokenRepository
                        .findByTokenHash(SecureTokens.sha256(token))
                        .filter(found -> found.getPurpose() == PURPOSE && found.isActiveAt(now))
                        .orElseThrow(EmailVerificationService::invalidToken);
        markVerified(emailToken, now);
    }

    @Transactional(noRollbackFor = BusinessRuleException.class)
    public void confirmByCode(String email, String code) {
        Instant now = clock.instant();
        EmailToken emailToken =
                emailTokenRepository
                        .findFirstByUserEmailAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
                                EmailAddresses.normalize(email), PURPOSE)
                        .filter(found -> found.isActiveAt(now))
                        .orElseThrow(EmailVerificationService::invalidToken);
        if (!SecureTokens.matches(code, emailToken.getCodeHash())) {
            emailToken.recordFailedAttempt(now, MAX_FAILED_ATTEMPTS);
            throw invalidToken();
        }
        markVerified(emailToken, now);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        Instant now = clock.instant();
        userRepository
                .findByEmail(EmailAddresses.normalize(email))
                .filter(User::isAwaitingEmailVerification)
                .filter(user -> isPastResendCooldown(user, now))
                .ifPresent(
                        user -> {
                            emailTokenRepository.invalidateUnusedTokens(user, PURPOSE, now);
                            sendVerificationEmail(user);
                        });
    }

    private boolean isPastResendCooldown(User user, Instant now) {
        Instant cooldownStart = now.minus(RESEND_COOLDOWN);
        return emailTokenRepository
                .findFirstByUserAndPurposeOrderByCreatedAtDesc(user, PURPOSE)
                .map(latest -> !latest.getCreatedAt().isAfter(cooldownStart))
                .orElse(true);
    }

    private static void markVerified(EmailToken emailToken, Instant now) {
        emailToken.markUsed(now);
        emailToken.getUser().markEmailVerified(now);
    }

    private static BusinessRuleException invalidToken() {
        return new BusinessRuleException(
                ErrorCode.TOKEN_INVALID_OR_EXPIRED, "Mã không hợp lệ hoặc đã hết hạn");
    }
}
