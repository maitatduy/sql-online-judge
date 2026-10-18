package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.EmailToken;
import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.PasswordResetEmailRequestedEvent;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    static final Duration RESET_LINK_TTL = Duration.ofMinutes(30);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);

    private static final EmailTokenPurpose PURPOSE = EmailTokenPurpose.RESET;

    private final UserRepository userRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public void requestReset(String email) {
        Instant now = clock.instant();
        userRepository
                .findByEmail(EmailAddresses.normalize(email))
                .filter(user -> !user.isDeleted())
                .filter(user -> isPastCooldown(user, now))
                .ifPresent(user -> issueResetLink(user, now));
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        Instant now = clock.instant();
        EmailToken resetToken =
                emailTokenRepository
                        .findByTokenHash(SecureTokens.sha256(token))
                        .filter(found -> isPasswordLink(found) && found.isActiveAt(now))
                        .orElseThrow(PasswordResetService::invalidLink);
        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        if (resetToken.getPurpose() == EmailTokenPurpose.INVITE) {
            user.markEmailVerified(now);
        }
        resetToken.markUsed(now);
        userSessionRepository.revokeAllActiveSessions(user, now);
    }

    private void issueResetLink(User user, Instant now) {
        emailTokenRepository.invalidateUnusedTokens(user, PURPOSE, now);
        String token = SecureTokens.newToken();
        EmailToken resetToken = new EmailToken();
        resetToken.setUser(user);
        resetToken.setPurpose(PURPOSE);
        resetToken.setTokenHash(SecureTokens.sha256(token));
        resetToken.setExpiresAt(now.plus(RESET_LINK_TTL));
        emailTokenRepository.save(resetToken);
        eventPublisher.publishEvent(
                new PasswordResetEmailRequestedEvent(
                        user.getId(), user.getEmail(), user.getUsername(), token));
    }

    private static boolean isPasswordLink(EmailToken token) {
        return token.getPurpose() == PURPOSE || token.getPurpose() == EmailTokenPurpose.INVITE;
    }

    private boolean isPastCooldown(User user, Instant now) {
        Instant cooldownStart = now.minus(RESEND_COOLDOWN);
        return emailTokenRepository
                .findFirstByUserAndPurposeOrderByCreatedAtDesc(user, PURPOSE)
                .map(latest -> !latest.getCreatedAt().isAfter(cooldownStart))
                .orElse(true);
    }

    private static BusinessRuleException invalidLink() {
        return new BusinessRuleException(
                ErrorCode.TOKEN_INVALID_OR_EXPIRED, "Liên kết không hợp lệ hoặc đã hết hạn");
    }
}
