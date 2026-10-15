package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.EmailToken;
import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.event.VerificationEmailRequestedEvent;
import com.sqljudge.auth.repository.EmailTokenRepository;
import java.time.Clock;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    static final Duration VERIFICATION_TTL = Duration.ofMinutes(15);

    private final EmailTokenRepository emailTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Transactional
    public void sendVerificationEmail(User user) {
        String token = SecureTokens.newToken();
        String code = SecureTokens.newCode();
        EmailToken emailToken = new EmailToken();
        emailToken.setUser(user);
        emailToken.setPurpose(EmailTokenPurpose.VERIFY);
        emailToken.setTokenHash(SecureTokens.sha256(token));
        emailToken.setCodeHash(SecureTokens.sha256(code));
        emailToken.setExpiresAt(clock.instant().plus(VERIFICATION_TTL));
        emailTokenRepository.save(emailToken);
        eventPublisher.publishEvent(
                new VerificationEmailRequestedEvent(
                        user.getId(), user.getEmail(), user.getUsername(), token, code));
    }
}
