package com.sqljudge.community.repository;

import com.sqljudge.community.entity.HiddenReason;
import com.sqljudge.community.entity.Post;
import com.sqljudge.community.entity.PostCategory;
import com.sqljudge.community.entity.PostStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, UUID> {

    String LISTED_POSTS =
            """
            FROM Post p
            WHERE p.status = :published AND p.deletedAt IS NULL
              AND (:category IS NULL OR p.category = :category)
              AND (:problemId IS NULL OR p.problemId = :problemId)
              AND (:unansweredOnly = FALSE OR p.commentCount = 0)
              AND (:tag IS NULL OR :tag MEMBER OF p.tags)
              AND (:titlePattern IS NULL OR LOWER(p.title) LIKE :titlePattern ESCAPE '!')
              AND (:savedBy IS NULL OR EXISTS (
                    SELECT 1 FROM Bookmark b WHERE b.postId = p.id AND b.userId = :savedBy))
            """;

    @Query(value = "SELECT p " + LISTED_POSTS, countQuery = "SELECT COUNT(p) " + LISTED_POSTS)
    Page<Post> findListed(
            @Param("published") PostStatus published,
            @Param("category") PostCategory category,
            @Param("problemId") UUID problemId,
            @Param("unansweredOnly") boolean unansweredOnly,
            @Param("tag") String tag,
            @Param("titlePattern") String titlePattern,
            @Param("savedBy") UUID savedBy,
            Pageable pageable);

    Optional<Post> findByIdAndDeletedAtIsNull(UUID id);

    List<Post> findByProblemIdInAndHiddenReason(
            Collection<UUID> problemIds, HiddenReason hiddenReason);

    @Query(
            "SELECT p.category AS category, COUNT(p) AS total FROM Post p"
                    + " WHERE p.status = :published AND p.deletedAt IS NULL GROUP BY p.category")
    List<CategoryCountRow> countByCategory(@Param("published") PostStatus published);
}
