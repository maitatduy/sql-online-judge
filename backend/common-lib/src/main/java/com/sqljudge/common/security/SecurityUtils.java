package com.sqljudge.common.security;

import java.util.Optional;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<CurrentUser> currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof CurrentUser currentUser) {
            return Optional.of(currentUser);
        }
        return Optional.empty();
    }

    public static CurrentUser requireCurrentUser() {
        return currentUser()
                .orElseThrow(
                        () ->
                                new AuthenticationCredentialsNotFoundException(
                                        "No authenticated user in security context"));
    }

    public static Optional<UUID> currentUserId() {
        return currentUser().map(CurrentUser::id);
    }
}
