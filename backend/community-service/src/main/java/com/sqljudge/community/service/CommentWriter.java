package com.sqljudge.community.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.BusinessRuleException;
import com.sqljudge.common.exception.ForbiddenException;
import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.common.outbox.OutboxWriter;
import com.sqljudge.common.persistence.UuidV7;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.community.dto.request.WriteCommentRequest;
import com.sqljudge.community.entity.Comment;
import com.sqljudge.community.entity.Comment.CommentThread;
import com.sqljudge.community.entity.HiddenReason;
import com.sqljudge.community.entity.Post;
import com.sqljudge.community.event.CommentedEvent;
import com.sqljudge.community.event.CommunityTopics;
import com.sqljudge.community.repository.CommentRepository;
import com.sqljudge.community.repository.PostRepository;
import java.time.Clock;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentWriter {

    private static final int EVENT_VERSION = 1;

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final OutboxWriter outboxWriter;
    private final Clock clock;

    @Transactional
    public UUID write(WriteCommentRequest request, boolean contestLocked) {
        CommentThread thread = new CommentThread(request.postId(), request.problemId());
        if (!thread.isValid()) {
            throw invalidTarget("Bình luận phải gắn với một bài viết hoặc một bài tập");
        }
        UUID authorId = SecurityUtils.requireCurrentUser().id();
        Post post = thread.postId() == null ? null : openPost(thread.postId());
        UUID recipientId = post == null ? null : post.getAuthorId();
        if (request.parentId() != null) {
            Comment parent = liveComment(request.parentId());
            if (parent.isReply() || !parent.thread().equals(thread)) {
                throw invalidTarget("Chỉ trả lời được bình luận gốc trong cùng chủ đề");
            }
            recipientId = parent.getAuthorId();
        }
        Comment comment =
                commentRepository.save(
                        Comment.write(thread, request.parentId(), authorId, request.body().trim()));
        if (contestLocked) {
            comment.hide(HiddenReason.CONTEST);
        }
        if (post != null) {
            post.commentAdded();
        }
        if (recipientId != null && !recipientId.equals(authorId)) {
            announce(comment, recipientId);
        }
        log.info("comment_created commentId={}", comment.getId());
        return comment.getId();
    }

    @Transactional
    public void edit(UUID commentId, String body) {
        Comment comment = liveComment(commentId);
        if (!comment.isWrittenBy(SecurityUtils.requireCurrentUser().id())) {
            throw forbidden();
        }
        comment.edit(body.trim());
    }

    @Transactional
    public void delete(UUID commentId) {
        Comment comment = liveComment(commentId);
        CurrentUser user = SecurityUtils.requireCurrentUser();
        boolean isModerator = user.hasRole(Role.MODERATOR) || user.hasRole(Role.ADMIN);
        if (!comment.isWrittenBy(user.id()) && !isModerator) {
            throw forbidden();
        }
        comment.delete(clock.instant());
        if (comment.getPostId() != null) {
            postRepository.findById(comment.getPostId()).ifPresent(Post::commentRemoved);
        }
        log.info("comment_deleted commentId={}", commentId);
    }

    Comment liveComment(UUID commentId) {
        return commentRepository
                .findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(
                        () ->
                                new NotFoundException(
                                        ErrorCode.COMMENT_NOT_FOUND, "Không tìm thấy bình luận"));
    }

    private Post openPost(UUID postId) {
        return postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .filter(Post::isVisibleToEveryone)
                .orElseThrow(PostErrors::notFound);
    }

    private void announce(Comment comment, UUID recipientId) {
        outboxWriter.append(
                CommunityTopics.COMMENTED,
                comment.getId().toString(),
                new CommentedEvent(
                        UuidV7.generate(),
                        clock.instant(),
                        EVENT_VERSION,
                        comment.getId(),
                        comment.getPostId(),
                        comment.getProblemId(),
                        comment.getAuthorId(),
                        recipientId));
    }

    private static BusinessRuleException invalidTarget(String message) {
        return new BusinessRuleException(ErrorCode.INVALID_COMMENT_TARGET, message);
    }

    private static ForbiddenException forbidden() {
        return new ForbiddenException(
                ErrorCode.ACCESS_DENIED, "Bạn không có quyền sửa bình luận này");
    }
}
