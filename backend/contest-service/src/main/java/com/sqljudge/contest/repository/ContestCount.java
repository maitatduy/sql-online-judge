package com.sqljudge.contest.repository;

import java.util.UUID;

public record ContestCount(UUID contestId, long total) {}
