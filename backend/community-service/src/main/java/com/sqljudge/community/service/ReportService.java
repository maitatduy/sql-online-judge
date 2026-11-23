package com.sqljudge.community.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ConflictException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.community.dto.request.CreateReportRequest;
import com.sqljudge.community.entity.Comment;
import com.sqljudge.community.entity.HiddenReason;
import com.sqljudge.community.entity.Post;
import com.sqljudge.community.entity.Report;
import com.sqljudge.community.entity.Report.ReportedContent;
import com.sqljudge.community.entity.ReportAction;
import com.sqljudge.community.entity.ReportStatus;
import com.sqljudge.community.entity.ReportTarget;
import com.sqljudge.community.event.CommunityTopics;
import com.sqljudge.community.event.ReportResolvedEvent;
import com.sqljudge.community.event.ReportedEvent;
import com.sqljudge.community.repository.CommentRepository;
import com.sqljudge.community.repository.PostRepository;
import com.sqljudge.community.repository.ReportRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    static final long AUTO_HIDE_THRESHOLD = 3;
    private static final int EVENT_VERSION = 1;

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    @Transactional
    public void report(CreateReportRequest request) {
        UUID reporterId = SecurityUtils.requireCurrentUser().id();
        if (reportRepository.existsByTargetTypeAndTargetIdAndReporterId(
                request.targetType(), request.targetId(), reporterId)) {
            throw new ConflictException(ErrorCode.ALREADY_REPORTED, "Bạn đã báo cáo nội dung này");
        }
        ReportedContent content = contentOf(request.targetType(), request.targetId());
        reportRepository.save(Report.open(content, reporterId, request.reason(), request.detail()));
        long openReports =
                reportRepository.countByTargetTypeAndTargetIdAndStatus(
                        content.targetType(), content.targetId(), ReportStatus.OPEN);
        if (openReports >= AUTO_HIDE_THRESHOLD) {
            hide(content);
        }
        outboxWriter.append(
                CommunityTopics.REPORTED,
                content.targetId().toString(),
                new ReportedEvent(
                        UuidV7.generate(),
                        clock.instant(),
                        EVENT_VERSION,
                        content.targetType(),
                        content.targetId(),
                        request.reason(),
                        openReports));
        log.info(
                "content_reported targetType={} targetId={} openReports={}",
                content.targetType(),
                content.targetId(),
                openReports);
    }

    @Transactional
    public void resolve(ReportTarget targetType, UUID targetId, ReportAction action, String note) {
        List<Report> open =
                reportRepository.findByTargetTypeAndTargetIdAndStatus(
                        targetType, targetId, ReportStatus.OPEN);
        if (open.isEmpty()) {
            throw new NotFoundException(
                    ErrorCode.REPORT_NOT_FOUND, "Không có báo cáo nào đang chờ xử lý");
        }
        UUID moderatorId = SecurityUtils.requireCurrentUser().id();
        open.forEach(report -> report.resolve(action, note, moderatorId, clock.instant()));
        applyToContent(targetType, targetId, action);
        outboxWriter.append(
                CommunityTopics.REPORT_RESOLVED,
                targetId.toString(),
                new ReportResolvedEvent(
                        UuidV7.generate(),
                        clock.instant(),
                        EVENT_VERSION,
                        targetType,
                        targetId,
                        open.get(0).getTargetAuthorId(),
                        action));
        log.info(
                "report_resolved targetType={} targetId={} action={}",
                targetType,
                targetId,
                action);
    }

    private void applyToContent(ReportTarget targetType, UUID targetId, ReportAction action) {
        if (targetType == ReportTarget.POST) {
            postRepository
                    .findByIdAndDeletedAtIsNull(targetId)
                    .ifPresent(
                            post -> {
                                if (action == ReportAction.DISMISS) {
                                    post.unhide(HiddenReason.REPORTS);
                                } else {
                                    post.delete(clock.instant());
                                }
                            });
        }
        if (targetType == ReportTarget.COMMENT) {
            commentRepository
                    .findByIdAndDeletedAtIsNull(targetId)
                    .ifPresent(
                            comment -> {
                                if (action == ReportAction.DISMISS) {
                                    comment.unhide(HiddenReason.REPORTS);
                                } else {
                                    removeComment(comment);
                                }
                            });
        }
    }

    private void removeComment(Comment comment) {
        comment.delete(clock.instant());
        if (comment.getPostId() != null) {
            postRepository.findById(comment.getPostId()).ifPresent(Post::commentRemoved);
        }
    }

    private void hide(ReportedContent content) {
        if (content.targetType() == ReportTarget.POST) {
            postRepository
                    .findById(content.targetId())
                    .ifPresent(post -> post.hide(HiddenReason.REPORTS));
        }
        if (content.targetType() == ReportTarget.COMMENT) {
            commentRepository
                    .findById(content.targetId())
                    .ifPresent(comment -> comment.hide(HiddenReason.REPORTS));
        }
    }

    private ReportedContent contentOf(ReportTarget targetType, UUID targetId) {
        UUID authorId =
                switch (targetType) {
                    case POST ->
                            postRepository
                                    .findByIdAndDeletedAtIsNull(targetId)
                                    .map(Post::getAuthorId)
                                    .orElseThrow(PostErrors::notFound);
                    case COMMENT ->
                            commentRepository
                                    .findByIdAndDeletedAtIsNull(targetId)
                                    .map(Comment::getAuthorId)
                                    .orElseThrow(
                                            () ->
                                                    new NotFoundException(
                                                            ErrorCode.COMMENT_NOT_FOUND,
                                                            "Không tìm thấy bình luận"));
                    case USER -> targetId;
                };
        return new ReportedContent(targetType, targetId, authorId);
    }
}
