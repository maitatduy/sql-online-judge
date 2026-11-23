package com.sqljudge.community.service;

import com.sqljudge.common.security.SecurityUtils;
import com.sqljudge.community.entity.Bookmark;
import com.sqljudge.community.entity.Post;
import com.sqljudge.community.repository.BookmarkRepository;
import com.sqljudge.community.repository.PostRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;

    @Transactional
    public void save(UUID postId) {
        postRepository
                .findByIdAndDeletedAtIsNull(postId)
                .filter(Post::isVisibleToEveryone)
                .orElseThrow(PostErrors::notFound);
        UUID userId = SecurityUtils.requireCurrentUser().id();
        if (!bookmarkRepository.existsByUserIdAndPostId(userId, postId)) {
            bookmarkRepository.save(Bookmark.of(postId, userId));
        }
    }

    @Transactional
    public void remove(UUID postId) {
        bookmarkRepository.deleteByUserIdAndPostId(SecurityUtils.requireCurrentUser().id(), postId);
    }
}
