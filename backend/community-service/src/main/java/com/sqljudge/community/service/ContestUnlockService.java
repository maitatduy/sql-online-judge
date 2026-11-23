package com.sqljudge.community.service;

import com.sqljudge.community.entity.Comment;
import com.sqljudge.community.entity.HiddenReason;
import com.sqljudge.community.entity.Post;
import com.sqljudge.community.repository.CommentRepository;
import com.sqljudge.community.repository.PostRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContestUnlockService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public void unlock(List<UUID> problemIds) {
        if (problemIds.isEmpty()) {
            return;
        }
        List<Post> posts =
                postRepository.findByProblemIdInAndHiddenReason(problemIds, HiddenReason.CONTEST);
        posts.forEach(post -> post.unhide(HiddenReason.CONTEST));
        List<Comment> comments =
                commentRepository.findByProblemIdInAndHiddenReason(
                        problemIds, HiddenReason.CONTEST);
        comments.forEach(comment -> comment.unhide(HiddenReason.CONTEST));
        log.info("contest_content_unlocked posts={} comments={}", posts.size(), comments.size());
    }
}
