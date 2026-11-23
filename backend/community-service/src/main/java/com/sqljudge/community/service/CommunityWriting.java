package com.sqljudge.community.service;

import com.sqljudge.community.client.ContestServiceClient;
import com.sqljudge.community.dto.request.SavePostRequest;
import com.sqljudge.community.dto.request.WriteCommentRequest;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityWriting {

    private final ContestServiceClient contestServiceClient;
    private final PostCommandService postCommandService;
    private final CommentWriter commentWriter;

    public UUID createPost(SavePostRequest request) {
        return postCommandService.create(request, isContestLocked(request.problemId()));
    }

    public void updatePost(UUID postId, SavePostRequest request) {
        postCommandService.update(postId, request, isContestLocked(request.problemId()));
    }

    public UUID writeComment(WriteCommentRequest request) {
        return commentWriter.write(request, isContestLocked(request.problemId()));
    }

    private boolean isContestLocked(UUID problemId) {
        return problemId != null
                && contestServiceClient.problemUsage(problemId).isInUnfinishedContest();
    }
}
