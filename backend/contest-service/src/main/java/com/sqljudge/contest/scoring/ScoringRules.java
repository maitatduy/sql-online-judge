package com.sqljudge.contest.scoring;

import java.time.Instant;

public record ScoringRules(Instant startAt, Instant freezeAt, int penaltyMinutes) {}
