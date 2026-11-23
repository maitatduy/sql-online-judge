package com.sqljudge.community.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqljudge.community.entity.ReportReason;
import com.sqljudge.community.entity.ReportStatus;
import com.sqljudge.community.entity.ReportTarget;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReportCaseDetailResponse(
        ReportTarget targetType,
        UUID targetId,
        UUID authorId,
        String authorUsername,
        long priorViolationCount,
        Content content,
        List<ReportItem> reports) {

    public record Content(
            String title,
            String body,
            UUID postId,
            UUID problemId,
            @JsonProperty("isHidden") boolean isHidden,
            @JsonProperty("isDeleted") boolean isDeleted) {}

    public record ReportItem(
            UUID id,
            UUID reporterId,
            String reporterUsername,
            ReportReason reason,
            String detail,
            ReportStatus status,
            Instant createdAt) {}
}
