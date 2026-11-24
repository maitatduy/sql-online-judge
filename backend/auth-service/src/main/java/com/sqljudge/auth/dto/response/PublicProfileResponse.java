package com.sqljudge.auth.dto.response;

import com.sqljudge.auth.entity.User;
import com.sqljudge.common.security.Role;
import java.time.Instant;
import java.util.UUID;

public record PublicProfileResponse(
        UUID userId,
        String username,
        String displayName,
        String avatarUrl,
        String bio,
        String school,
        String location,
        String websiteUrl,
        Role role,
        Instant joinedAt) {

    public static PublicProfileResponse from(User user) {
        return new PublicProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getSchool(),
                user.getLocation(),
                user.getWebsiteUrl(),
                user.getRole(),
                user.getCreatedAt());
    }
}
