package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AuthResponse;
import com.sqljudge.auth.dto.response.AuthUserResponse;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserSession;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.UnauthenticatedException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SignInService {

    static final Duration SESSION_TTL = Duration.ofDays(14);
    private static final int MAX_USER_AGENT_LENGTH = 255;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AccessTokenIssuer accessTokenIssuer;
    private final Clock clock;

    @Transactional
    public SignInResult signIn(UUID userId, boolean isPersistent, String userAgent) {
        Instant now = clock.instant();
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(
                                () ->
                                        new UnauthenticatedException(
                                                ErrorCode.UNAUTHORIZED,
                                                "Bạn cần đăng nhập để tiếp tục"));
        user.setLastActiveAt(now);
        String refreshToken = SecureTokens.newToken();
        userSessionRepository.save(newSession(user, refreshToken, isPersistent, userAgent, now));
        AuthResponse response =
                new AuthResponse(
                        accessTokenIssuer.issue(user),
                        new AuthUserResponse(
                                user.getId(), user.getUsername(), user.getEmail(), user.getRole()));
        return new SignInResult(response, refreshToken, isPersistent);
    }

    private static UserSession newSession(
            User user, String refreshToken, boolean isPersistent, String userAgent, Instant now) {
        UserSession session = new UserSession();
        session.setUser(user);
        session.setTokenHash(SecureTokens.sha256(refreshToken));
        session.setPersistent(isPersistent);
        session.setUserAgent(truncate(userAgent));
        session.setExpiresAt(now.plus(SESSION_TTL));
        session.setLastUsedAt(now);
        return session;
    }

    private static String truncate(String userAgent) {
        if (userAgent == null || userAgent.length() <= MAX_USER_AGENT_LENGTH) {
            return userAgent;
        }
        return userAgent.substring(0, MAX_USER_AGENT_LENGTH);
    }
}
