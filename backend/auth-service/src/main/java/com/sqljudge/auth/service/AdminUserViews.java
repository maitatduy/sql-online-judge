package com.sqljudge.auth.service;

import com.sqljudge.auth.dto.response.AccountStatus;
import com.sqljudge.auth.dto.response.AdminUserDetailResponse;
import com.sqljudge.auth.dto.response.AdminUserSummaryResponse;
import com.sqljudge.auth.entity.User;
import java.time.Instant;
import java.util.List;

final class AdminUserViews {

    private static final int STAT_NOT_TRACKED_YET = 0;
    private static final boolean PRESENCE_NOT_TRACKED_YET = false;
    private static final String SCHOOL_NOT_STORED_YET = null;

    private AdminUserViews() {}

    static AdminUserSummaryResponse toSummary(User user, Instant now) {
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                STAT_NOT_TRACKED_YET,
                STAT_NOT_TRACKED_YET,
                user.getCreatedAt(),
                user.getLastActiveAt(),
                PRESENCE_NOT_TRACKED_YET,
                user.isBannedAt(now) ? AccountStatus.BANNED : AccountStatus.ACTIVE,
                user.getBannedUntil(),
                user.isEmailVerified(),
                user.getAvatarUrl(),
                user.getDeletedAt());
    }

    static AdminUserDetailResponse toDetail(User user, Instant now) {
        return new AdminUserDetailResponse(
                toSummary(user, now),
                user.getDisplayName(),
                SCHOOL_NOT_STORED_YET,
                user.getSignupProvider(),
                STAT_NOT_TRACKED_YET,
                STAT_NOT_TRACKED_YET,
                List.of());
    }
}
