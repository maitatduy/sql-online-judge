package com.sqljudge.community.repository;

import com.sqljudge.community.entity.Comment;
import com.sqljudge.community.entity.CommentStatus;
import com.sqljudge.community.entity.HiddenReason;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

    @Query(
            "SELECT c FROM Comment c WHERE c.parentId IS NULL AND c.deletedAt IS NULL"
                    + " AND c.status = :visible"
                    + " AND ((:postId IS NOT NULL AND c.postId = :postId)"
                    + " OR (:problemId IS NOT NULL AND c.problemId = :problemId))")
    Page<Comment> findTopLevel(
            @Param("postId") UUID postId,
            @Param("problemId") UUID problemId,
            @Param("visible") CommentStatus visible,
            Pageable pageable);

    @Query(
            "SELECT c FROM Comment c WHERE c.parentId IN :parentIds AND c.deletedAt IS NULL"
                    + " AND c.status = :visible ORDER BY c.createdAt")
    List<Comment> findReplies(
            @Param("parentIds") Collection<UUID> parentIds,
            @Param("visible") CommentStatus visible);

    Optional<Comment> findByIdAndDeletedAtIsNull(UUID id);

    @Query(
            "SELECT c FROM Comment c WHERE c.problemId IS NOT NULL AND c.deletedAt IS NULL"
                    + " AND c.status = :visible ORDER BY c.createdAt DESC, c.id DESC")
    List<Comment> findLatestOnProblems(@Param("visible") CommentStatus visible, Limit limit);

    List<Comment> findByProblemIdInAndHiddenReason(
            Collection<UUID> problemIds, HiddenReason hiddenReason);
}
