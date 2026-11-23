package com.sqljudge.community.dto.response;

import java.util.UUID;

public record ContributorResponse(UUID userId, String username, long points) {}
