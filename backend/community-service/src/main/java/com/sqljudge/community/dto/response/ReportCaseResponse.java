package com.sqljudge.community.dto.response;

import com.sqljudge.community.entity.ReportReason;
import com.sqljudge.community.entity.ReportTarget;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ReportCaseResponse(
        ReportTarget targetType,
        UUID targetId,
        UUID authorId,
        String authorUsername,
        String preview,
        long reportCount,
        Map<ReportReason, Long> reasons,
        Instant latestReportAt) {}
