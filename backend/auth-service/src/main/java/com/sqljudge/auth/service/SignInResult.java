package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AuthResponse;

public record SignInResult(AuthResponse response, String refreshToken, boolean isPersistent) {

    @Override
    public String toString() {
        return "SignInResult[userId=" + response.user().id() + "]";
    }
}
