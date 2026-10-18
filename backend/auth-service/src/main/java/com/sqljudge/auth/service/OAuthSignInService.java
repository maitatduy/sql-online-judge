package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.OAuthAccount;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.auth.repository.OAuthAccountRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ForbiddenException;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthSignInService {

    private static final boolean REMEMBER_OAUTH_SESSIONS = true;

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final UsernameGenerator usernameGenerator;
    private final SignInService signInService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public SignInResult signIn(OAuthProfile profile, String userAgent) {
        try {
            return signInOnce(profile, userAgent);
        } catch (DataIntegrityViolationException firstConflict) {
            log.info("oauth_sign_in_retry provider={}", profile.provider());
        }
        try {
            return signInOnce(profile, userAgent);
        } catch (DataIntegrityViolationException secondConflict) {
            log.warn("oauth_sign_in_conflict provider={}", profile.provider());
            throw new ForbiddenException(
                    ErrorCode.OAUTH_LOGIN_FAILED, "Không đăng nhập được bằng tài khoản này");
        }
    }

    private SignInResult signInOnce(OAuthProfile profile, String userAgent) {
        return transactionTemplate.execute(status -> signInInTransaction(profile, userAgent));
    }

    private SignInResult signInInTransaction(OAuthProfile profile, String userAgent) {
        Instant now = clock.instant();
        User user =
                oauthAccountRepository
                        .findByProviderAndProviderUserId(
                                profile.provider(), profile.providerUserId())
                        .map(OAuthAccount::getUser)
                        .orElseGet(() -> linkOrCreateUser(profile, now));
        ensureCanSignIn(user, now);
        return signInService.signIn(user.getId(), REMEMBER_OAUTH_SESSIONS, userAgent);
    }

    private User linkOrCreateUser(OAuthProfile profile, Instant now) {
        if (!profile.isEmailVerified() || profile.email() == null) {
            throw new ForbiddenException(
                    ErrorCode.OAUTH_EMAIL_NOT_VERIFIED,
                    "Email của tài khoản liên kết chưa được xác minh");
        }
        String email = EmailAddresses.normalize(profile.email());
        User user =
                userRepository.findByEmail(email).orElseGet(() -> createUser(profile, email, now));
        if (!user.isEmailVerified()) {
            discardUnverifiedCredentials(user, now);
        }
        user.markEmailVerified(now);
        OAuthAccount account = new OAuthAccount();
        account.setUser(user);
        account.setProvider(profile.provider());
        account.setProviderUserId(profile.providerUserId());
        oauthAccountRepository.save(account);
        return user;
    }

    private void discardUnverifiedCredentials(User user, Instant now) {
        user.setPasswordHash(null);
        userSessionRepository.revokeAllActiveSessions(user, now);
        emailTokenRepository.invalidateAllUnusedTokens(user, now);
    }

    private User createUser(OAuthProfile profile, String email, Instant now) {
        User user = new User();
        user.setUsername(usernameGenerator.generate(profile.login()));
        user.setEmail(email);
        user.setDisplayName(profile.displayName());
        user.setRole(Role.LEARNER);
        user.setSignupProvider(profile.provider());
        user.setEmailVerifiedAt(now);
        return userRepository.saveAndFlush(user);
    }

    private static void ensureCanSignIn(User user, Instant now) {
        if (user.isDeleted()) {
            throw new ForbiddenException(
                    ErrorCode.OAUTH_LOGIN_FAILED, "Không đăng nhập được bằng tài khoản này");
        }
        if (user.isBannedAt(now)) {
            throw new ForbiddenException(
                    ErrorCode.ACCOUNT_BANNED, "Tài khoản của bạn đang bị khoá");
        }
    }
}
