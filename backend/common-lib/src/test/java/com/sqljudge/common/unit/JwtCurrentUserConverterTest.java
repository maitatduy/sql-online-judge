package com.sqljudge.common.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.JwtCurrentUserConverter;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;

class JwtCurrentUserConverterTest {

    private static final UUID USER_ID = UUID.fromString("0192f3a0-0000-7000-8000-000000000001");

    private final JwtCurrentUserConverter converter = new JwtCurrentUserConverter();

    @Test
    @DisplayName("Claim của JWT trở thành CurrentUser kèm authority theo vai trò")
    void convert_validClaims_returnsCurrentUserWithRoleAuthority() {
        AbstractAuthenticationToken authentication =
                converter.convert(jwt(USER_ID.toString(), "minh.anh", "SETTER"));

        assertThat(authentication.isAuthenticated()).isTrue();
        assertThat(authentication.getPrincipal())
                .isEqualTo(new CurrentUser(USER_ID, "minh.anh", Set.of(Role.SETTER)));
        assertThat(authentication.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SETTER");
    }

    @Test
    @DisplayName("Vai trò không hợp lệ thì token bị coi là không hợp lệ (401), không phải lỗi 500")
    void convert_unknownRole_throwsInvalidBearerToken() {
        assertThatThrownBy(() -> converter.convert(jwt(USER_ID.toString(), "minh.anh", "ROOT")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    @Test
    @DisplayName("Thiếu vai trò hoặc subject không phải UUID thì token không hợp lệ")
    void convert_missingOrMalformedClaims_throwsInvalidBearerToken() {
        assertThatThrownBy(() -> converter.convert(jwt(USER_ID.toString(), "minh.anh", null)))
                .isInstanceOf(InvalidBearerTokenException.class);
        assertThatThrownBy(() -> converter.convert(jwt("not-a-uuid", "minh.anh", "LEARNER")))
                .isInstanceOf(InvalidBearerTokenException.class);
    }

    private static Jwt jwt(String subject, String username, String role) {
        Jwt.Builder builder =
                Jwt.withTokenValue("token")
                        .header("alg", "RS256")
                        .subject(subject)
                        .claim(JwtCurrentUserConverter.USERNAME_CLAIM, username)
                        .issuedAt(Instant.parse("2026-10-15T00:00:00Z"))
                        .expiresAt(Instant.parse("2026-10-15T00:15:00Z"));
        if (role != null) {
            builder.claim(JwtCurrentUserConverter.ROLE_CLAIM, role);
        }
        return builder.build();
    }
}
