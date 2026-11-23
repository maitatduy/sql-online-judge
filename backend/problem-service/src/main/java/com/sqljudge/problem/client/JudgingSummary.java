package com.sqljudge.problem.client;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JudgingSummary(long submissionCount, long acceptedCount, List<UserJudging> users) {

    public JudgingSummary {
        users = List.copyOf(users);
    }

    public long solvedUserCount() {
        return users.stream().filter(user -> user.firstAcceptedAt() != null).count();
    }

    public record UserJudging(UUID userId, Instant firstAcceptedAt) {}
}
