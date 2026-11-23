package com.sqljudge.community.service;

import com.sqljudge.community.dto.response.ReportCaseDetailResponse.Content;
import com.sqljudge.community.entity.Comment;
import com.sqljudge.community.entity.CommentStatus;
import com.sqljudge.community.entity.Post;
import com.sqljudge.community.entity.PostStatus;
import com.sqljudge.community.entity.Report;
import com.sqljudge.community.entity.ReportAction;
import com.sqljudge.community.entity.ReportReason;
import com.sqljudge.community.entity.ReportStatus;
import com.sqljudge.community.entity.ReportTarget;
import com.sqljudge.community.repository.CommentRepository;
import com.sqljudge.community.repository.PostRepository;
import com.sqljudge.community.repository.ReportCaseRow;
import com.sqljudge.community.repository.ReportRepository;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportCaseReader {

    private static final int PREVIEW_LENGTH = 160;

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional(readOnly = true)
    public CasePage cases(ReportStatus status, ReportTarget targetType, Pageable pageable) {
        Page<ReportCaseRow> rows = reportRepository.findCases(status, targetType, pageable);
        List<UUID> targetIds = rows.map(ReportCaseRow::targetId).toList();
        Map<UUID, Map<ReportReason, Long>> reasons =
                targetIds.isEmpty()
                        ? Map.of()
                        : reportRepository.findByTargetIdInAndStatus(targetIds, status).stream()
                                .collect(
                                        Collectors.groupingBy(
                                                Report::getTargetId,
                                                Collectors.groupingBy(
                                                        Report::getReason,
                                                        () -> new EnumMap<>(ReportReason.class),
                                                        Collectors.counting())));
        Map<UUID, String> previews =
                rows.stream()
                        .collect(
                                Collectors.toMap(
                                        ReportCaseRow::targetId,
                                        row -> previewOf(row.targetType(), row.targetId()),
                                        (first, second) -> first));
        return new CasePage(rows, reasons, previews);
    }

    @Transactional(readOnly = true)
    public CaseDetail detail(ReportTarget targetType, UUID targetId) {
        List<Report> reports =
                reportRepository.findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
                        targetType, targetId);
        if (reports.isEmpty()) {
            return null;
        }
        UUID authorId = reports.get(0).getTargetAuthorId();
        return new CaseDetail(
                authorId,
                reportRepository.countRemovedContentOf(authorId, ReportAction.REMOVE),
                contentOf(targetType, targetId),
                reports);
    }

    private Content contentOf(ReportTarget targetType, UUID targetId) {
        if (targetType == ReportTarget.POST) {
            return postRepository
                    .findById(targetId)
                    .map(
                            post ->
                                    new Content(
                                            post.getTitle(),
                                            post.getBodyMarkdown(),
                                            post.getId(),
                                            post.getProblemId(),
                                            post.getStatus() == PostStatus.HIDDEN,
                                            post.getDeletedAt() != null))
                    .orElse(null);
        }
        if (targetType == ReportTarget.COMMENT) {
            return commentRepository
                    .findById(targetId)
                    .map(
                            comment ->
                                    new Content(
                                            null,
                                            comment.getBody(),
                                            comment.getPostId(),
                                            comment.getProblemId(),
                                            comment.getStatus() == CommentStatus.HIDDEN,
                                            comment.getDeletedAt() != null))
                    .orElse(null);
        }
        return null;
    }

    private String previewOf(ReportTarget targetType, UUID targetId) {
        String text =
                switch (targetType) {
                    case POST -> postRepository.findById(targetId).map(Post::getTitle).orElse("");
                    case COMMENT ->
                            commentRepository.findById(targetId).map(Comment::getBody).orElse("");
                    case USER -> "";
                };
        return text.length() <= PREVIEW_LENGTH ? text : text.substring(0, PREVIEW_LENGTH) + "…";
    }

    public record CasePage(
            Page<ReportCaseRow> rows,
            Map<UUID, Map<ReportReason, Long>> reasons,
            Map<UUID, String> previews) {}

    public record CaseDetail(
            UUID authorId, long priorViolationCount, Content content, List<Report> reports) {}
}
