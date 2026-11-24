package com.sqljudge.ai.repository;

import com.sqljudge.ai.entity.AiMessage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {

    List<AiMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    List<AiMessage> findByConversationIdOrderByCreatedAtDesc(
            UUID conversationId, Pageable pageable);

    boolean existsByConversationId(UUID conversationId);
}
