package com.sqljudge.ai.repository;

import com.sqljudge.ai.entity.AiMessage;
import com.sqljudge.ai.entity.MessageRole;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {

    List<AiMessage> findByConversationIdOrderByCreatedAtAscRoleDesc(UUID conversationId);

    List<AiMessage> findByConversationIdOrderByCreatedAtDescRoleAsc(
            UUID conversationId, Pageable pageable);

    boolean existsByConversationId(UUID conversationId);

    long countByRoleAndCreatedAtGreaterThanEqual(MessageRole role, Instant since);

    long countByBlockedTrueAndCreatedAtGreaterThanEqual(Instant since);

    @Query(
            "SELECT COUNT(DISTINCT m.conversation.userId) FROM AiMessage m"
                    + " WHERE m.role = :role AND m.createdAt >= :since")
    long countUsersSince(@Param("role") MessageRole role, @Param("since") Instant since);

    @EntityGraph(attributePaths = "conversation")
    Page<AiMessage> findByBlockedTrueOrderByCreatedAtDesc(Pageable pageable);

    List<AiMessage> findByIdIn(Collection<UUID> ids);
}
