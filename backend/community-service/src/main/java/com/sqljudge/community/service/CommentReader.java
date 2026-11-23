package com.sqljudge.community.service;

import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.community.entity.Comment;
import com.sqljudge.community.entity.CommentStatus;
import com.sqljudge.community.entity.Vote;
import com.sqljudge.community.entity.VoteTarget;
import com.sqljudge.community.repository.CommentRepository;
import com.sqljudge.community.repository.VoteRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentReader {

    private final CommentRepository commentRepository;
    private final VoteRepository voteRepository;

    @Transactional(readOnly = true)
    public CommentPage read(CommentSearch search, int page, int size) {
        Sort sort =
                search.sort() == CommentSort.TOP
                        ? Sort.by(Sort.Order.desc("voteScore"), Sort.Order.asc("createdAt"))
                        : Sort.by(Sort.Order.desc("createdAt"));
        Page<CommentView> topLevel =
                commentRepository
                        .findTopLevel(
                                search.postId(),
                                search.problemId(),
                                CommentStatus.VISIBLE,
                                PageRequest.of(page - 1, size, sort))
                        .map(CommentView::of);
        List<CommentView> replies =
                topLevel.isEmpty()
                        ? List.of()
                        : commentRepository
                                .findReplies(
                                        topLevel.map(CommentView::id).toList(),
                                        CommentStatus.VISIBLE)
                                .stream()
                                .map(CommentView::of)
                                .toList();
        Set<UUID> commentIds = new HashSet<>(topLevel.map(CommentView::id).toList());
        replies.forEach(reply -> commentIds.add(reply.id()));
        Map<UUID, Integer> myVotes =
                SecurityUtils.currentUserId()
                        .filter(userId -> !commentIds.isEmpty())
                        .map(
                                userId ->
                                        voteRepository
                                                .findByTargetTypeAndTargetIdInAndUserId(
                                                        VoteTarget.COMMENT, commentIds, userId)
                                                .stream()
                                                .collect(
                                                        Collectors.toMap(
                                                                Vote::getTargetId, Vote::getValue)))
                        .orElse(Map.of());
        return new CommentPage(
                new PageImpl<>(
                        topLevel.getContent(), topLevel.getPageable(), topLevel.getTotalElements()),
                replies,
                myVotes);
    }

    public record CommentView(
            UUID id,
            UUID postId,
            UUID problemId,
            UUID parentId,
            UUID authorId,
            String body,
            int voteScore,
            Instant createdAt,
            Instant updatedAt) {

        static CommentView of(Comment comment) {
            return new CommentView(
                    comment.getId(),
                    comment.getPostId(),
                    comment.getProblemId(),
                    comment.getParentId(),
                    comment.getAuthorId(),
                    comment.getBody(),
                    comment.getVoteScore(),
                    comment.getCreatedAt(),
                    comment.getUpdatedAt());
        }
    }

    public record CommentPage(
            Page<CommentView> topLevel, List<CommentView> replies, Map<UUID, Integer> myVotes) {

        List<CommentView> repliesOf(UUID parentId) {
            return replies.stream().filter(reply -> parentId.equals(reply.parentId())).toList();
        }

        Set<UUID> authorIds() {
            List<UUID> ids = new ArrayList<>(topLevel.map(CommentView::authorId).toList());
            replies.forEach(reply -> ids.add(reply.authorId()));
            return new HashSet<>(ids);
        }
    }
}
