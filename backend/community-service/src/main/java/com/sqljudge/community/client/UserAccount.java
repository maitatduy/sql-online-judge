package com.sqljudge.community.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.common.security.Role;
import java.util.UUID;

public record UserAccount(
        UUID userId, String username, Role role, @JsonProperty("isActive") boolean isActive) {

    public boolean isActiveSetter() {
        return isActive && role == Role.SETTER;
    }
}
