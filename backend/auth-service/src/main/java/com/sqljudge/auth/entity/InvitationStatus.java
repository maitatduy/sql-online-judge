package com.sqljudge.auth.entity;

import java.time.Instant;

public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED;

    public static InvitationStatus of(
            Instant acceptedAt, Instant revokedAt, Instant expiresAt, Instant now) {
        if (acceptedAt != null) {
            return ACCEPTED;
        }
        if (revokedAt != null) {
            return REVOKED;
        }
        return now.isBefore(expiresAt) ? PENDING : EXPIRED;
    }
}
