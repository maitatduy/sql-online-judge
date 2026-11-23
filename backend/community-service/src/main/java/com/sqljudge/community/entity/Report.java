package com.sqljudge.community.entity;

import com.sqljudge.common.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "reports",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_reports_target_type_target_id_reporter_id",
                        columnNames = {"target_type", "target_id", "reporter_id"}),
        indexes = {
            @Index(name = "idx_reports_status_created_at", columnList = "status, created_at"),
            @Index(name = "idx_reports_target_author_id", columnList = "target_author_id")
        })
@Getter
@NoArgsConstructor
public class Report extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 16)
    private ReportTarget targetType;

    @Column(name = "target_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID targetId;

    @Column(name = "target_author_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID targetAuthorId;

    @Column(name = "reporter_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ReportReason reason;

    @Column(name = "detail", length = 500)
    private String detail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ReportStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution", length = 16)
    private ReportAction resolution;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "resolved_by", columnDefinition = "BINARY(16)")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    public static Report open(
            ReportedContent content, UUID reporterId, ReportReason reason, String detail) {
        Report report = new Report();
        report.targetType = content.targetType();
        report.targetId = content.targetId();
        report.targetAuthorId = content.authorId();
        report.reporterId = reporterId;
        report.reason = reason;
        report.detail = detail;
        report.status = ReportStatus.OPEN;
        return report;
    }

    public void resolve(ReportAction action, String resolutionNote, UUID moderatorId, Instant now) {
        status = action == ReportAction.DISMISS ? ReportStatus.DISMISSED : ReportStatus.RESOLVED;
        resolution = action;
        note = resolutionNote;
        resolvedBy = moderatorId;
        resolvedAt = now;
    }

    public record ReportedContent(ReportTarget targetType, UUID targetId, UUID authorId) {}
}
