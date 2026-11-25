package com.sqljudge.auth.entity;

import java.time.Instant;

public record InvitationTimeline(
        Instant acceptedAt, Instant revokedAt, Instant declinedAt, Instant expiresAt) {

    public InvitationStatus statusAt(Instant now) {
        if (acceptedAt != null) {
            return InvitationStatus.ACCEPTED;
        }
        if (revokedAt != null) {
            return InvitationStatus.REVOKED;
        }
        if (declinedAt != null) {
            return InvitationStatus.DECLINED;
        }
        return now.isBefore(expiresAt) ? InvitationStatus.PENDING : InvitationStatus.EXPIRED;
    }
}
