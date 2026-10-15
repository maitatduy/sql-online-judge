package com.sqljudge.auth.unit;

import static org.assertj.core.api.Assertions.assertThat;

import com.sqljudge.auth.controller.RefreshTokenCookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

class RefreshTokenCookieTest {

    @Test
    @DisplayName(
            "Ghi nhớ đăng nhập: cookie httpOnly, Secure, SameSite=Lax, Path=/api/auth, sống 14"
                    + " ngày")
    void persistentSession_hasFourteenDayMaxAge() {
        ResponseCookie cookie = RefreshTokenCookie.of("refresh-token", true);

        assertThat(cookie.getName()).isEqualTo("sqljudge_refresh");
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge().toDays()).isEqualTo(14);
    }

    @Test
    @DisplayName("Không ghi nhớ: cookie phiên, đóng trình duyệt là mất")
    void sessionOnlyCookie_hasNoMaxAge() {
        ResponseCookie cookie = RefreshTokenCookie.of("refresh-token", false);

        assertThat(cookie.getMaxAge().isNegative()).isTrue();
        assertThat(cookie.toString()).doesNotContain("Max-Age");
    }
}
