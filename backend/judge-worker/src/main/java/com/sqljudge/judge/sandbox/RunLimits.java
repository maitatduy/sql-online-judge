package com.sqljudge.judge.sandbox;

import java.time.Duration;

public record RunLimits(Duration timeout, int maxRows, long maxResultBytes) {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofMillis(2000);
    private static final long MAX_TIMEOUT_MS = 6_000;
    private static final int DEFAULT_MAX_ROWS = 10_000;
    private static final long DEFAULT_MAX_RESULT_BYTES = 5L * 1024 * 1024;

    public static RunLimits defaults() {
        return new RunLimits(DEFAULT_TIMEOUT, DEFAULT_MAX_ROWS, DEFAULT_MAX_RESULT_BYTES);
    }

    public static RunLimits forTimeLimit(int timeLimitMs) {
        Duration timeout =
                timeLimitMs > 0
                        ? Duration.ofMillis(Math.min(timeLimitMs, MAX_TIMEOUT_MS))
                        : DEFAULT_TIMEOUT;
        return new RunLimits(timeout, DEFAULT_MAX_ROWS, DEFAULT_MAX_RESULT_BYTES);
    }
}
