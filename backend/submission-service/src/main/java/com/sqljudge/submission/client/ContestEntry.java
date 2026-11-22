package com.sqljudge.submission.client;

import java.util.UUID;

public record ContestEntry(UUID contestId, UUID problemId, String letter) {}
