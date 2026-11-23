package com.sqljudge.community.repository;

import com.sqljudge.community.entity.ReportTarget;
import java.time.Instant;
import java.util.UUID;

public record ReportCaseRow(
        ReportTarget targetType,
        UUID targetId,
        UUID targetAuthorId,
        long reportCount,
        Instant latestReportAt) {}
