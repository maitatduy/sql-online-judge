package com.sqljudge.common.security;

public enum Role {
    LEARNER,
    SETTER,
    MODERATOR,
    ADMIN;

    private static final String AUTHORITY_PREFIX = "ROLE_";

    public String authority() {
        return AUTHORITY_PREFIX + name();
    }
}
