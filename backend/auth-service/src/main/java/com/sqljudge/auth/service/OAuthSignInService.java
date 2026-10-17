package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.OAuthAccount;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.OAuthAccountRepository;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ForbiddenException;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OAuthSignInService {

    private static final boolean REMEMBER_OAUTH_SESSIONS = true;

    private final OAuthAccountRepository oauthAccountRepository;
    private final UserRepository userRepository;
    private final UsernameGenerator usernameGenerator;
    private final SignInService signInService;
    private final Clock clock;

    @Transactional
    public SignInResult signIn(OAuthProfile profile, String userAgent) {
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
        user.markEmailVerified(now);
        OAuthAccount account = new OAuthAccount();
        account.setUser(user);
        account.setProvider(profile.provider());
        account.setProviderUserId(profile.providerUserId());
        oauthAccountRepository.save(account);
        return user;
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
