package com.sqljudge.notification.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public record UserContact(
        UUID userId,
        String username,
        String email,
        @JsonProperty("isEmailVerified") boolean isEmailVerified) {}
