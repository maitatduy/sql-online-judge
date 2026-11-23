package com.sqljudge.auth.dto.response;

import java.util.UUID;

public record InternalUserNameResponse(UUID userId, String username, String displayName) {}
