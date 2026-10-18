package com.sqljudge.auth.dto.response;

import java.util.List;

public record UserSessionsResponse(List<UserSessionResponse> items) {}
