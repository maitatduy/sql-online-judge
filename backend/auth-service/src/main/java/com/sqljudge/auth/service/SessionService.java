package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserSession;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.UnauthenticatedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    static final Duration CONCURRENT_REFRESH_GRACE = Duration.ofSeconds(10);

    private final UserSessionRepository userSessionRepository;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;

    @Transactional(noRollbackFor = UnauthenticatedException.class)
    public SignInResult refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw invalidRefreshToken();
        }
        Instant now = clock.instant();
        String tokenHash = SecureTokens.sha256(refreshToken);
        UserSession current = userSessionRepository.findByTokenHash(tokenHash).orElse(null);
        if (current != null) {
            return rotate(current, now);
        }
        UserSession rotatedAway =
                userSessionRepository
                        .findByPreviousTokenHash(tokenHash)
                        .orElseThrow(SessionService::invalidRefreshToken);
        return handlePreviousToken(rotatedAway, now);
    }

    @Transactional
    public void revoke(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        Instant now = clock.instant();
        userSessionRepository
                .findByTokenHash(SecureTokens.sha256(refreshToken))
                .ifPresent(session -> session.revoke(now));
    }

    private SignInResult rotate(UserSession session, Instant now) {
        User user = requireUsableSession(session, now);
        String newRefreshToken = SecureTokens.newToken();
        session.rotateTo(SecureTokens.sha256(newRefreshToken), now);
        user.setLastActiveAt(now);
        return new SignInResult(
                AuthResponses.of(user, accessTokenIssuer.issue(user)),
                newRefreshToken,
                session.isPersistent());
    }

    private SignInResult handlePreviousToken(UserSession session, Instant now) {
        if (!session.wasRotatedWithin(CONCURRENT_REFRESH_GRACE, now)) {
            session.revoke(now);
            log.warn("refresh_token_reuse_detected sessionId={}", session.getId());
            throw invalidRefreshToken();
        }
        User user = requireUsableSession(session, now);
        return new SignInResult(
                AuthResponses.of(user, accessTokenIssuer.issue(user)),
                null,
                session.isPersistent());
    }

    private static User requireUsableSession(UserSession session, Instant now) {
        if (!session.isActiveAt(now)) {
            throw invalidRefreshToken();
        }
        User user = session.getUser();
        if (!user.canSignInAt(now)) {
            session.revoke(now);
            throw invalidRefreshToken();
        }
        return user;
    }

    private static UnauthenticatedException invalidRefreshToken() {
        return new UnauthenticatedException(
                ErrorCode.INVALID_REFRESH_TOKEN,
                "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại");
    }
}
