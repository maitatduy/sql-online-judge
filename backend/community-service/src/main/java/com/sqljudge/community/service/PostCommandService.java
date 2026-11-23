package com.sqljudge.community.service;

import com.sqljudge.common.error.ErrorCode;
import com.sqljudge.common.exception.ForbiddenException;
import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.community.dto.request.SavePostRequest;
import com.sqljudge.community.entity.Post;
import com.sqljudge.community.entity.Post.PostContent;
import com.sqljudge.community.repository.PostRepository;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostCommandService {

    private final PostRepository postRepository;
    private final Clock clock;

    @Transactional
    public UUID create(SavePostRequest request) {
        Post post = Post.draft(SecurityUtils.requireCurrentUser().id(), contentOf(request));
        if (request.publish()) {
            post.publish(clock.instant());
        }
        UUID postId = postRepository.save(post).getId();
        log.info("post_created postId={} published={}", postId, request.publish());
        return postId;
    }

    @Transactional
    public void update(UUID postId, SavePostRequest request) {
        Post post = livePost(postId);
        if (!post.isWrittenBy(SecurityUtils.requireCurrentUser().id())) {
            throw forbidden();
        }
        post.update(contentOf(request));
        if (request.publish()) {
            post.publish(clock.instant());
        }
    }

    @Transactional
    public void delete(UUID postId) {
        Post post = livePost(postId);
        CurrentUser user = SecurityUtils.requireCurrentUser();
        boolean isModerator = user.hasRole(Role.MODERATOR) || user.hasRole(Role.ADMIN);
        if (!post.isWrittenBy(user.id()) && !isModerator) {
            throw forbidden();
        }
        post.delete(clock.instant());
        log.info("post_deleted postId={}", postId);
    }

    private Post livePost(UUID postId) {
        return postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(PostErrors::notFound);
    }

    private static PostContent contentOf(SavePostRequest request) {
        return new PostContent(
                request.category(),
                request.title().trim(),
                request.bodyMarkdown(),
                request.problemId(),
                new LinkedHashSet<>(request.tags()));
    }

    private static ForbiddenException forbidden() {
        return new ForbiddenException(
                ErrorCode.ACCESS_DENIED, "Bạn không có quyền sửa bài viết này");
    }
}
