package com.sqljudge.auth.dto.request;

import java.time.Duration;
import java.time.Instant;

public enum BanDuration {
    DAYS_7(Duration.ofDays(7)),
    DAYS_30(Duration.ofDays(30)),
    PERMANENT(null);

    private final Duration length;

    BanDuration(Duration length) {
        this.length = length;
    }

    public Instant bannedUntilFrom(Instant start) {
        return length == null ? null : start.plus(length);
    }
}
