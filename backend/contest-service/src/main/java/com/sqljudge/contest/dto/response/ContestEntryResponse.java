package com.sqljudge.contest.dto.response;

import java.util.UUID;

public record ContestEntryResponse(UUID contestId, UUID problemId, String letter) {}
