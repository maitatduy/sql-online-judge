package com.sqljudge.community.service;

import com.sqljudge.common.exception.NotFoundException;
import com.sqljudge.community.client.ProblemRef;
import com.sqljudge.community.client.ProblemServiceClient;
import com.sqljudge.community.dto.response.ProblemDiscussionResponse;
import com.sqljudge.community.entity.Comment;
import com.sqljudge.community.entity.CommentStatus;
import com.sqljudge.community.repository.CommentRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProblemDiscussionService {

    private static final int EXCERPT_LENGTH = 160;
    private static final Pattern MARKDOWN_SYMBOLS = Pattern.compile("[`*_>#~\\[\\]]");
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final CommentRepository commentRepository;
    private final ProblemServiceClient problemServiceClient;
    private final AuthorNames authorNames;

    public List<ProblemDiscussionResponse> latest(int size) {
        List<Comment> comments =
                commentRepository.findLatestOnProblems(CommentStatus.VISIBLE, Limit.of(size));
        Map<UUID, Optional<ProblemRef>> problems = new HashMap<>();
        comments.forEach(
                comment -> problems.computeIfAbsent(comment.getProblemId(), this::problemOf));
        Map<UUID, String> authors =
                authorNames.of(comments.stream().map(Comment::getAuthorId).distinct().toList());
        return comments.stream()
                .filter(comment -> problems.get(comment.getProblemId()).isPresent())
                .map(
                        comment ->
                                toResponse(
                                        comment,
                                        problems.get(comment.getProblemId()).orElseThrow(),
                                        authors.get(comment.getAuthorId())))
                .toList();
    }

    private Optional<ProblemRef> problemOf(UUID problemId) {
        try {
            return Optional.of(problemServiceClient.problem(problemId));
        } catch (NotFoundException removedProblem) {
            return Optional.empty();
        }
    }

    private static ProblemDiscussionResponse toResponse(
            Comment comment, ProblemRef problem, String authorUsername) {
        return new ProblemDiscussionResponse(
                comment.getId(),
                comment.getProblemId(),
                problem.slug(),
                problem.title(),
                comment.getAuthorId(),
                authorUsername,
                excerptOf(comment.getBody()),
                comment.getParentId() != null,
                comment.getCreatedAt());
    }

    static String excerptOf(String body) {
        String plain =
                WHITESPACE
                        .matcher(MARKDOWN_SYMBOLS.matcher(body).replaceAll(""))
                        .replaceAll(" ")
                        .trim();
        return plain.length() <= EXCERPT_LENGTH
                ? plain
                : plain.substring(0, EXCERPT_LENGTH).stripTrailing() + "…";
    }
}
