package com.sqljudge.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.auth.entity.User;

public record AccountProfileResponse(
        String username,
        String email,
        String displayName,
        String bio,
        String school,
        String location,
        String websiteUrl,
        String avatarUrl,
        @JsonProperty("hasPassword") boolean hasPassword) {

    public static AccountProfileResponse from(User user) {
        return new AccountProfileResponse(
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getBio(),
                user.getSchool(),
                user.getLocation(),
                user.getWebsiteUrl(),
                user.getAvatarUrl(),
                user.getPasswordHash() != null);
    }
}
