package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.AdminActionType;
import java.time.Instant;

public record AuditLogSearch(
        String targetUsername, String actorUsername, AdminActionType action, Instant from) {}
