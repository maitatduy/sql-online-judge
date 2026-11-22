package com.sqljudge.contest.entity;

import java.util.UUID;

public record ContestCellKey(UUID contestId, UUID userId, UUID problemId) {}
