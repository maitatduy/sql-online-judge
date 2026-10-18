package com.sqljudge.gateway.error;

import java.time.Instant;

public record ApiError(int status, String code, String message, Instant timestamp) {}
