package com.sqljudge.auth.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserNoteResponse(UUID id, String authorUsername, String body, Instant createdAt) {}
