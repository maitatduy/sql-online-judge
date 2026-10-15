package com.sqljudge.common.security;

import java.util.Set;
import java.util.UUID;

public record CurrentUser(UUID id, String username, Set<Role> roles) {

    public CurrentUser {
        roles = Set.copyOf(roles);
    }

    public boolean hasRole(Role role) {
        return roles.contains(role);
    }
}
