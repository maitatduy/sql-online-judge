package com.sqljudge.auth.dto.response;

public record UserCountsResponse(
        long total, long unverified, long setters, long moderators, long banned) {}
