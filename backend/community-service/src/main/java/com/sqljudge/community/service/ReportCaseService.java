package com.sqljudge.community.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.web.PageResponse;
import com.sqljudge.community.dto.response.ReportCaseDetailResponse;
import com.sqljudge.community.dto.response.ReportCaseDetailResponse.ReportItem;
import com.sqljudge.community.dto.response.ReportCaseResponse;
import com.sqljudge.community.entity.Report;
import com.sqljudge.community.entity.ReportStatus;
import com.sqljudge.community.entity.ReportTarget;
import com.sqljudge.community.repository.ReportCaseRow;
import com.sqljudge.community.service.ReportCaseReader.CaseDetail;
import com.sqljudge.community.service.ReportCaseReader.CasePage;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportCaseService {

    private final ReportCaseReader reader;
    private final AuthorNames authorNames;

    public PageResponse<ReportCaseResponse> cases(
            ReportStatus status, ReportTarget targetType, int page, int size) {
        CasePage cases = reader.cases(status, targetType, PageRequest.of(page - 1, size));
        Map<UUID, String> names =
                authorNames.of(cases.rows().map(ReportCaseRow::targetAuthorId).toSet());
        return PageResponse.from(
                cases.rows(),
                row ->
                        new ReportCaseResponse(
                                row.targetType(),
                                row.targetId(),
                                row.targetAuthorId(),
                                names.get(row.targetAuthorId()),
                                cases.previews().get(row.targetId()),
                                row.reportCount(),
                                cases.reasons().getOrDefault(row.targetId(), Map.of()),
                                row.latestReportAt()));
    }

    public ReportCaseDetailResponse detail(ReportTarget targetType, UUID targetId) {
        CaseDetail detail = reader.detail(targetType, targetId);
        if (detail == null) {
            throw new NotFoundException(ErrorCode.REPORT_NOT_FOUND, "Không tìm thấy báo cáo");
        }
        Set<UUID> userIds = new HashSet<>();
        userIds.add(detail.authorId());
        detail.reports().forEach(report -> userIds.add(report.getReporterId()));
        Map<UUID, String> names = authorNames.of(userIds);
        return new ReportCaseDetailResponse(
                targetType,
                targetId,
                detail.authorId(),
                names.get(detail.authorId()),
                detail.priorViolationCount(),
                detail.content(),
                detail.reports().stream().map(report -> itemOf(report, names)).toList());
    }

    private static ReportItem itemOf(Report report, Map<UUID, String> names) {
        return new ReportItem(
                report.getId(),
                report.getReporterId(),
                names.get(report.getReporterId()),
                report.getReason(),
                report.getDetail(),
                report.getStatus(),
                report.getCreatedAt());
    }
}
