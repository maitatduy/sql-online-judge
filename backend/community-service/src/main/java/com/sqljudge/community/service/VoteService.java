package com.sqljudge.community.service;

import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.community.dto.request.VoteRequest;
import com.sqljudge.community.dto.response.VoteResponse;
import com.sqljudge.community.entity.Comment;
import com.sqljudge.community.entity.Post;
import com.sqljudge.community.entity.Vote;
import com.sqljudge.community.entity.VoteTarget;
import com.sqljudge.community.repository.PostRepository;
import com.sqljudge.community.repository.VoteRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final PostRepository postRepository;
    private final CommentWriter commentWriter;

    @Transactional
    public VoteResponse vote(VoteRequest request) {
        UUID userId = SecurityUtils.requireCurrentUser().id();
        if (request.targetType() == VoteTarget.POST) {
            Post post =
                    postRepository
                            .findByIdAndDeletedAtIsNull(request.targetId())
                            .filter(Post::isVisibleToEveryone)
                            .orElseThrow(PostErrors::notFound);
            post.addVotes(change(request, post.getAuthorId(), userId));
            return new VoteResponse(post.getVoteScore(), request.value());
        }
        Comment comment = commentWriter.liveComment(request.targetId());
        comment.addVotes(change(request, comment.getAuthorId(), userId));
        return new VoteResponse(comment.getVoteScore(), request.value());
    }

    private int change(VoteRequest request, UUID targetAuthorId, UUID userId) {
        Vote vote =
                voteRepository
                        .findByTargetTypeAndTargetIdAndUserId(
                                request.targetType(), request.targetId(), userId)
                        .orElseGet(
                                () ->
                                        Vote.cast(
                                                request.targetType(),
                                                request.targetId(),
                                                targetAuthorId,
                                                userId));
        int delta = vote.changeTo(request.value());
        if (request.value() == 0) {
            if (vote.getId() != null) {
                voteRepository.delete(vote);
            }
        } else {
            voteRepository.save(vote);
        }
        return delta;
    }
}
