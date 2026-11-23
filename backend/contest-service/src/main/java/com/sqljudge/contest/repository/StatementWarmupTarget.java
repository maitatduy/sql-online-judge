package com.sqljudge.contest.repository;

import java.util.UUID;

public record StatementWarmupTarget(UUID contestId, UUID problemId) {}
