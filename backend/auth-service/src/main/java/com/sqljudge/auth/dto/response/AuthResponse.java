package com.sqljudge.auth.dto.response;

public record AuthResponse(String accessToken, AuthUserResponse user) {}
