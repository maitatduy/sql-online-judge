package com.sqljudge.auth.dto.response;

public record UserRestrictionsResponse(int aiDailyQuota, boolean isBannedFromRatedContests) {}
