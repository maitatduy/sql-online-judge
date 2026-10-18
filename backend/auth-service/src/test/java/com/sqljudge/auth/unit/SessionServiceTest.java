package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserSession;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.auth.service.AccessTokenIssuer;
import com.sqljudge.auth.service.SecureTokens;
import com.sqljudge.auth.service.SessionService;
import com.sqljudge.auth.service.SignInResult;
import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.UnauthenticatedException;
import com.sqljudge.common.security.Role;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T05:00:00Z");
    private static final String CURRENT_TOKEN = "current-refresh-token";
    private static final String PREVIOUS_TOKEN = "previous-refresh-token";

    @Mock private UserSessionRepository userSessionRepository;

    private SessionService sessionService;
    private User user;
    private UserSession session;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        sessionService =
                new SessionService(
                        userSessionRepository,
                        new AccessTokenIssuer(TestJwts.newKey(), clock),
                        clock);
        user = new User();
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.setUsername("minh.anh");
        user.setEmail("minh.anh@gmail.com");
        user.setRole(Role.LEARNER);
        user.setEmailVerifiedAt(NOW.minus(Duration.ofDays(1)));
        session = new UserSession();
        session.setUser(user);
        session.setTokenHash(SecureTokens.sha256(CURRENT_TOKEN));
        session.setPersistent(true);
        session.setExpiresAt(NOW.plus(Duration.ofDays(7)));
        session.setLastUsedAt(NOW.minus(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("Refresh token còn hạn: xoay vòng sang token mới, giữ lựa chọn ghi nhớ")
    void refresh_activeToken_rotatesToken() {
        stubCurrent(CURRENT_TOKEN, session);

        SignInResult result = sessionService.refresh(CURRENT_TOKEN);

        assertThat(result.refreshToken()).isNotBlank().isNotEqualTo(CURRENT_TOKEN);
        assertThat(result.isPersistent()).isTrue();
        assertThat(result.response().accessToken()).isNotBlank();
        assertThat(result.response().user().username()).isEqualTo("minh.anh");
        assertThat(session.getTokenHash()).isEqualTo(SecureTokens.sha256(result.refreshToken()));
        assertThat(session.getPreviousTokenHash()).isEqualTo(SecureTokens.sha256(CURRENT_TOKEN));
        assertThat(session.getRotatedAt()).isEqualTo(NOW);
        assertThat(session.getLastUsedAt()).isEqualTo(NOW);
        assertThat(user.getLastActiveAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Thiếu cookie, token lạ, phiên hết hạn hoặc đã thu hồi: 401 INVALID_REFRESH_TOKEN")
    void refresh_missingUnknownExpiredOrRevoked_rejects() {
        when(userSessionRepository.findByTokenHash(SecureTokens.sha256("unknown")))
                .thenReturn(Optional.empty());
        when(userSessionRepository.findByPreviousTokenHash(SecureTokens.sha256("unknown")))
                .thenReturn(Optional.empty());
        UserSession expired = copyOf(session, "expired");
        expired.setExpiresAt(NOW);
        UserSession revoked = copyOf(session, "revoked");
        revoked.setRevokedAt(NOW.minusSeconds(1));
        stubCurrent("expired", expired);
        stubCurrent("revoked", revoked);

        assertInvalid(null);
        assertInvalid(" ");
        assertInvalid("unknown");
        assertInvalid("expired");
        assertInvalid("revoked");
    }

    @Test
    @DisplayName(
            "Hai tab cùng refresh: token cũ trong 10 giây vẫn nhận access token, không xoay tiếp")
    void refresh_previousTokenWithinGrace_issuesAccessTokenOnly() {
        rotatedSecondsAgo(5);
        when(userSessionRepository.findByTokenHash(SecureTokens.sha256(PREVIOUS_TOKEN)))
                .thenReturn(Optional.empty());
        when(userSessionRepository.findByPreviousTokenHash(SecureTokens.sha256(PREVIOUS_TOKEN)))
                .thenReturn(Optional.of(session));

        SignInResult result = sessionService.refresh(PREVIOUS_TOKEN);

        assertThat(result.response().accessToken()).isNotBlank();
        assertThat(result.refreshToken()).isNull();
        assertThat(session.getTokenHash()).isEqualTo(SecureTokens.sha256(CURRENT_TOKEN));
        assertThat(session.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("Token cũ bị dùng lại sau 10 giây: coi là bị đánh cắp, thu hồi cả phiên")
    void refresh_previousTokenAfterGrace_revokesSession() {
        rotatedSecondsAgo(11);
        when(userSessionRepository.findByTokenHash(SecureTokens.sha256(PREVIOUS_TOKEN)))
                .thenReturn(Optional.empty());
        when(userSessionRepository.findByPreviousTokenHash(SecureTokens.sha256(PREVIOUS_TOKEN)))
                .thenReturn(Optional.of(session));

        assertInvalid(PREVIOUS_TOKEN);

        assertThat(session.getRevokedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName(
            "Tài khoản bị khoá, bị xoá hoặc chưa xác thực sau khi đăng nhập: thu hồi phiên, 401")
    void refresh_userNoLongerAllowed_revokesSession() {
        user.setBannedAt(NOW.minus(Duration.ofMinutes(5)));
        stubCurrent(CURRENT_TOKEN, session);

        assertInvalid(CURRENT_TOKEN);

        assertThat(session.getRevokedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Đăng xuất: thu hồi phiên của cookie; cookie lạ thì bỏ qua")
    void revoke_currentToken_revokesSession() {
        stubCurrent(CURRENT_TOKEN, session);
        when(userSessionRepository.findByTokenHash(SecureTokens.sha256("unknown")))
                .thenReturn(Optional.empty());

        sessionService.revoke(CURRENT_TOKEN);
        sessionService.revoke("unknown");
        sessionService.revoke(null);

        assertThat(session.getRevokedAt()).isEqualTo(NOW);
    }

    private void rotatedSecondsAgo(long seconds) {
        session.setPreviousTokenHash(SecureTokens.sha256(PREVIOUS_TOKEN));
        session.setRotatedAt(NOW.minusSeconds(seconds));
    }

    private void stubCurrent(String token, UserSession found) {
        when(userSessionRepository.findByTokenHash(SecureTokens.sha256(token)))
                .thenReturn(Optional.of(found));
    }

    private UserSession copyOf(UserSession source, String token) {
        UserSession copy = new UserSession();
        copy.setUser(source.getUser());
        copy.setTokenHash(SecureTokens.sha256(token));
        copy.setExpiresAt(source.getExpiresAt());
        copy.setLastUsedAt(source.getLastUsedAt());
        return copy;
    }

    private void assertInvalid(String token) {
        assertThatThrownBy(() -> sessionService.refresh(token))
                .isInstanceOf(UnauthenticatedException.class)
                .extracting("code")
                .isEqualTo(ErrorCode.INVALID_REFRESH_TOKEN);
    }
}
