package com.sqljudge.submission.client;

import java.util.Optional;
import java.util.UUID;

public record UserAccount(UUID userId, String username) {

    public static UserAccount missing(String username) {
        return new UserAccount(null, username);
    }

    public Optional<UUID> id() {
        return Optional.ofNullable(userId);
    }
}
