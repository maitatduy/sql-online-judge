package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.EmailTokenPurpose;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.repository.EmailTokenRepository;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.QuotaExceededException;
import java.time.Duration;
import java.time.Instant;

final class ResendCooldown {

    static final Duration WINDOW = Duration.ofSeconds(60);

    private ResendCooldown() {}

    static Duration remaining(
            EmailTokenRepository emailTokenRepository,
            User user,
            EmailTokenPurpose purpose,
            Instant now) {
        return emailTokenRepository
                .findFirstByUserAndPurposeOrderByCreatedAtDesc(user, purpose)
                .map(latest -> Duration.between(now, latest.getCreatedAt().plus(WINDOW)))
                .filter(wait -> wait.compareTo(Duration.ZERO) > 0)
                .orElse(Duration.ZERO);
    }

    static void requirePassed(
            EmailTokenRepository emailTokenRepository,
            User user,
            EmailTokenPurpose purpose,
            Instant now) {
        Duration wait = remaining(emailTokenRepository, user, purpose, now);
        if (!wait.isZero()) {
            throw new QuotaExceededException(
                    ErrorCode.EMAIL_RESEND_COOLDOWN,
                    "Vừa gửi email cho tài khoản này, vui lòng chờ rồi thử lại",
                    wait);
        }
    }
}
