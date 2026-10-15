package com.sqljudge.auth.controller;

import com.sqljudge.auth.service.SignInResult;
import java.time.Duration;
import org.springframework.http.ResponseCookie;

public final class RefreshTokenCookie {

    public static final String NAME = "sqljudge_refresh";
    private static final String PATH = "/api/auth";
    private static final Duration PERSISTENT_MAX_AGE = Duration.ofDays(14);

    private RefreshTokenCookie() {}

    public static ResponseCookie of(String refreshToken, boolean isPersistent) {
        ResponseCookie.ResponseCookieBuilder builder =
                ResponseCookie.from(NAME, refreshToken)
                        .httpOnly(true)
                        .secure(true)
                        .sameSite("Lax")
                        .path(PATH);
        if (isPersistent) {
            builder.maxAge(PERSISTENT_MAX_AGE);
        }
        return builder.build();
    }

    public static ResponseCookie cleared() {
        return ResponseCookie.from(NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path(PATH)
                .maxAge(Duration.ZERO)
                .build();
    }

    static String headerFor(SignInResult result) {
        return of(result.refreshToken(), result.isPersistent()).toString();
    }
}
