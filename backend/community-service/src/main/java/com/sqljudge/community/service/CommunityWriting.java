package com.sqljudge.community.service;

import com.sqljudge.community.client.ContestServiceClient;
import com.sqljudge.community.client.ProblemRef;
import com.sqljudge.community.client.ProblemServiceClient;
import com.sqljudge.community.dto.request.SavePostRequest;
import com.sqljudge.community.dto.request.WriteCommentRequest;
import com.sqljudge.community.entity.LinkedProblem;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommunityWriting {

    private final ContestServiceClient contestServiceClient;
    private final ProblemServiceClient problemServiceClient;
    private final PostCommandService postCommandService;
    private final CommentWriter commentWriter;

    public UUID createPost(SavePostRequest request) {
        return postCommandService.create(
                request, linkedProblem(request.problemId()), isContestLocked(request.problemId()));
    }

    public void updatePost(UUID postId, SavePostRequest request) {
        postCommandService.update(
                postId,
                request,
                linkedProblem(request.problemId()),
                isContestLocked(request.problemId()));
    }

    public UUID writeComment(WriteCommentRequest request) {
        return commentWriter.write(request, isContestLocked(request.problemId()));
    }

    private LinkedProblem linkedProblem(UUID problemId) {
        if (problemId == null) {
            return null;
        }
        ProblemRef problem = problemServiceClient.problem(problemId);
        return new LinkedProblem(problemId, problem.slug(), problem.title());
    }

    private boolean isContestLocked(UUID problemId) {
        return problemId != null
                && contestServiceClient.problemUsage(problemId).isInUnfinishedContest();
    }
}
