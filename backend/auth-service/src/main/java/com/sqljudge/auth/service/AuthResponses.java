package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AuthResponse;
import com.sqljudge.auth.dto.response.AuthUserResponse;
import com.sqljudge.auth.entity.User;

final class AuthResponses {

    private AuthResponses() {}

    static AuthResponse of(User user, String accessToken) {
        return new AuthResponse(
                accessToken,
                new AuthUserResponse(
                        user.getId(), user.getUsername(), user.getEmail(), user.getRole()));
    }
}
