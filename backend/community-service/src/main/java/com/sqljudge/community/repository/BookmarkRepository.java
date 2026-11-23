package com.sqljudge.community.repository;

import com.sqljudge.community.entity.Bookmark;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookmarkRepository extends JpaRepository<Bookmark, UUID> {

    boolean existsByUserIdAndPostId(UUID userId, UUID postId);

    long deleteByUserIdAndPostId(UUID userId, UUID postId);
}
