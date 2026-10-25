package com.sqljudge.auth.service;

import com.sqljudge.auth.entity.AdminActionType;
import com.sqljudge.auth.entity.AuditCategory;
import java.time.Instant;

public record AuditLogSearch(
        String targetUsername,
        String actorUsername,
        AdminActionType action,
        AuditCategory category,
        String keyword,
        Instant from) {}
