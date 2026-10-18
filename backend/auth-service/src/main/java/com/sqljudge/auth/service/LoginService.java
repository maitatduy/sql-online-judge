package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.request.LoginRequest;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ForbiddenException;
import com.sqljudge.common.exception.QuotaExceededException;
import com.sqljudge.common.exception.UnauthenticatedException;
import java.time.Clock;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private static final String TIMING_EQUALIZER_PASSWORD = "sqljudge-timing-equalizer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SignInService signInService;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final Clock clock;
    private final String timingEqualizerHash;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            SignInService signInService,
            LoginAttemptLimiter loginAttemptLimiter,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.signInService = signInService;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.clock = clock;
        this.timingEqualizerHash = passwordEncoder.encode(TIMING_EQUALIZER_PASSWORD);
    }

    @Transactional
    public SignInResult login(LoginRequest request, String userAgent) {
        String email = EmailAddresses.normalize(request.email());
        loginAttemptLimiter
                .lockRemaining(email)
                .ifPresent(
                        remaining -> {
                            throw new QuotaExceededException(
                                    ErrorCode.LOGIN_RATE_LIMITED,
                                    "Bạn đã nhập sai quá nhiều lần, vui lòng thử lại sau",
                                    remaining);
                        });
        User user =
                userRepository
                        .findByEmail(email)
                        .filter(found -> found.getPasswordHash() != null)
                        .orElse(null);
        String hash = user == null ? timingEqualizerHash : user.getPasswordHash();
        boolean isPasswordCorrect = passwordEncoder.matches(request.password(), hash);
        if (user == null || !isPasswordCorrect || user.isDeleted()) {
            loginAttemptLimiter.recordFailure(email);
            throw new UnauthenticatedException(
                    ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không đúng");
        }
        loginAttemptLimiter.reset(email);
        if (!user.isEmailVerified()) {
            throw new ForbiddenException(
                    ErrorCode.EMAIL_NOT_VERIFIED,
                    "Email chưa được xác thực. Hãy nhập mã đã gửi tới email của bạn");
        }
        if (user.isBannedAt(clock.instant())) {
            throw new ForbiddenException(
                    ErrorCode.ACCOUNT_BANNED, "Tài khoản của bạn đang bị khoá");
        }
        return signInService.signIn(user.getId(), request.rememberMe(), userAgent);
    }
}
