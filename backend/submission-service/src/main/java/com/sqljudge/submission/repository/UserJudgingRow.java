package com.sqljudge.submission.repository;

import java.time.Instant;
import java.util.UUID;

public record UserJudgingRow(UUID userId, Instant firstAcceptedAt) {}
