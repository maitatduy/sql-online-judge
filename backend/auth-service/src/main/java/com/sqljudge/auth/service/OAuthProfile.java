package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.SignupProvider;

public record OAuthProfile(
        SignupProvider provider,
        String providerUserId,
        String email,
        boolean isEmailVerified,
        String login,
        String displayName) {

    @Override
    public String toString() {
        return "OAuthProfile[provider=" + provider + ", providerUserId=" + providerUserId + "]";
    }
}
