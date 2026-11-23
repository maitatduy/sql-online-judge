package com.sqljudge.community.service;

import com.sqljudge.common.web.PageResponse;
import com.sqljudge.community.dto.response.CommentResponse;
import com.sqljudge.community.service.CommentReader.CommentPage;
import com.sqljudge.community.service.CommentReader.CommentView;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentQueryService {

    private final CommentReader commentReader;
    private final AuthorNames authorNames;

    public PageResponse<CommentResponse> list(CommentSearch search, int page, int size) {
        CommentPage comments = commentReader.read(search, page, size);
        Map<UUID, String> names = authorNames.of(comments.authorIds());
        return PageResponse.from(
                comments.topLevel(),
                comment ->
                        toResponse(
                                comment,
                                names,
                                comments.myVotes(),
                                comments.repliesOf(comment.id()).stream()
                                        .map(
                                                reply ->
                                                        toResponse(
                                                                reply,
                                                                names,
                                                                comments.myVotes(),
                                                                List.of()))
                                        .toList()));
    }

    private static CommentResponse toResponse(
            CommentView comment,
            Map<UUID, String> names,
            Map<UUID, Integer> myVotes,
            List<CommentResponse> replies) {
        return new CommentResponse(
                comment.id(),
                comment.postId(),
                comment.problemId(),
                comment.parentId(),
                comment.authorId(),
                names.get(comment.authorId()),
                comment.body(),
                comment.voteScore(),
                myVotes.getOrDefault(comment.id(), 0),
                comment.createdAt(),
                comment.updatedAt(),
                replies);
    }
}
