package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.jwk.RSAKey;
import com.sqljudge.auth.entity.User;
import com.sqljudge.auth.entity.UserSession;
import com.sqljudge.auth.repository.UserRepository;
import com.sqljudge.auth.repository.UserSessionRepository;
import com.sqljudge.auth.service.AccessTokenIssuer;
import com.sqljudge.auth.service.SecureTokens;
import com.sqljudge.auth.service.SignInResult;
import com.sqljudge.auth.service.SignInService;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SignInServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-15T05:00:00Z");
    private static final UUID USER_ID = UUID.fromString("0192f3a0-0000-7000-8000-000000000001");

    @Mock private UserRepository userRepository;
    @Mock private UserSessionRepository userSessionRepository;

    private final RSAKey signingKey = TestJwts.newKey();
    private SignInService signInService;
    private User user;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        signInService =
                new SignInService(
                        userRepository,
                        userSessionRepository,
                        new AccessTokenIssuer(signingKey, clock),
                        clock);
        user = new User();
        ReflectionTestUtils.setField(user, "id", USER_ID);
        user.setUsername("minh.anh");
        user.setEmail("minh.anh@gmail.com");
        user.setRole(Role.SETTER);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userSessionRepository.save(any(UserSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Access token RS256 sống 15 phút, mang sub, username, role và kid của khoá")
    void signIn_issuesShortLivedAccessToken() throws Exception {
        SignInResult result = signInService.signIn(USER_ID, false, "Mozilla/5.0");

        Jwt jwt = decodeIgnoringExpiry(result.response().accessToken());
        assertThat(jwt.getHeaders()).containsEntry("kid", signingKey.getKeyID());
        assertThat(jwt.getSubject()).isEqualTo(USER_ID.toString());
        assertThat(jwt.getClaimAsString("username")).isEqualTo("minh.anh");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("SETTER");
        assertThat(jwt.getId()).isNotBlank();
        assertThat(jwt.getIssuedAt()).isEqualTo(NOW);
        assertThat(jwt.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(15)));
    }

    @Test
    @DisplayName("Response chứa thông tin người dùng đúng hợp đồng frontend")
    void signIn_returnsUserSummary() {
        SignInResult result = signInService.signIn(USER_ID, false, "Mozilla/5.0");

        assertThat(result.response().user().id()).isEqualTo(USER_ID);
        assertThat(result.response().user().username()).isEqualTo("minh.anh");
        assertThat(result.response().user().email()).isEqualTo("minh.anh@gmail.com");
        assertThat(result.response().user().role()).isEqualTo(Role.SETTER);
        assertThat(user.getLastActiveAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("Phiên chỉ lưu hash của refresh token, hết hạn sau 14 ngày, cắt User-Agent dài")
    void signIn_createsSessionWithHashedRefreshToken() {
        String longUserAgent = "x".repeat(400);

        SignInResult result = signInService.signIn(USER_ID, true, longUserAgent);

        ArgumentCaptor<UserSession> saved = ArgumentCaptor.forClass(UserSession.class);
        verify(userSessionRepository).save(saved.capture());
        UserSession session = saved.getValue();
        assertThat(result.refreshToken()).matches("[A-Za-z0-9_-]{43}");
        assertThat(session.getTokenHash()).isEqualTo(SecureTokens.sha256(result.refreshToken()));
        assertThat(session.getUser()).isSameAs(user);
        assertThat(session.isPersistent()).isTrue();
        assertThat(result.isPersistent()).isTrue();
        assertThat(session.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(14)));
        assertThat(session.getLastUsedAt()).isEqualTo(NOW);
        assertThat(session.getUserAgent()).hasSize(255);
    }

    private Jwt decodeIgnoringExpiry(String token) throws Exception {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withPublicKey(signingKey.toRSAPublicKey()).build();
        decoder.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());
        return decoder.decode(token);
    }
}
