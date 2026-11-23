package com.sqljudge.community.service;

import com.sqljudge.common.security.CurrentUser;
import com.sqljudge.common.security.Role;
import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.community.entity.Post;
import com.sqljudge.community.entity.PostStatus;
import com.sqljudge.community.entity.Vote;
import com.sqljudge.community.entity.VoteTarget;
import com.sqljudge.community.repository.BookmarkRepository;
import com.sqljudge.community.repository.PostRepository;
import com.sqljudge.community.repository.VoteRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostReader {

    private final PostRepository postRepository;
    private final VoteRepository voteRepository;
    private final BookmarkRepository bookmarkRepository;

    @Transactional(readOnly = true)
    public Page<PostSnapshot> listed(PostSearch search, Pageable pageable) {
        return postRepository
                .findListed(
                        PostStatus.PUBLISHED,
                        search.category(),
                        search.problemId(),
                        search.tab() == PostTab.UNANSWERED,
                        search.tag(),
                        LikePatterns.containingIgnoreCase(search.keyword()),
                        savedByOf(search.tab()),
                        pageable)
                .map(PostSnapshot::of);
    }

    @Transactional(readOnly = true)
    public PostSnapshot readable(UUID postId) {
        Post post =
                postRepository.findByIdAndDeletedAtIsNull(postId).orElseThrow(PostErrors::notFound);
        if (post.isVisibleToEveryone() || canSeeUnlisted(post)) {
            return PostSnapshot.of(post);
        }
        throw PostErrors.notFound();
    }

    @Transactional(readOnly = true)
    public ViewerState viewerState(UUID postId) {
        return SecurityUtils.currentUserId()
                .map(
                        userId ->
                                new ViewerState(
                                        voteRepository
                                                .findByTargetTypeAndTargetIdAndUserId(
                                                        VoteTarget.POST, postId, userId)
                                                .map(Vote::getValue)
                                                .orElse(0),
                                        bookmarkRepository.existsByUserIdAndPostId(userId, postId)))
                .orElse(new ViewerState(0, false));
    }

    private static UUID savedByOf(PostTab tab) {
        if (tab != PostTab.SAVED) {
            return null;
        }
        return SecurityUtils.currentUserId().orElseGet(UUID::randomUUID);
    }

    private static boolean canSeeUnlisted(Post post) {
        Optional<CurrentUser> viewer = SecurityUtils.currentUser();
        if (viewer.isEmpty()) {
            return false;
        }
        if (post.isWrittenBy(viewer.get().id())) {
            return true;
        }
        return post.getStatus() == PostStatus.HIDDEN
                && (viewer.get().hasRole(Role.MODERATOR) || viewer.get().hasRole(Role.ADMIN));
    }

    public record ViewerState(int myVote, boolean isBookmarked) {}
}
