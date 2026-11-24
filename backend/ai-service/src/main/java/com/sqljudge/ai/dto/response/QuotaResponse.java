package com.sqljudge.ai.dto.response;

import java.time.Instant;

public record QuotaResponse(int used, int limit, Instant resetsAt) {}
