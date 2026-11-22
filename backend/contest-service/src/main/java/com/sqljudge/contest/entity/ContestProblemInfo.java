package com.sqljudge.contest.entity;

import java.util.UUID;

public record ContestProblemInfo(UUID problemId, String slug, String title, String difficulty) {}
